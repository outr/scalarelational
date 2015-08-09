package org.scalarelational.versioning

import pl.metastack.metarx._
import org.powerscala.concurrent.Time
import org.powerscala.property.Property
import org.scalarelational.extra.PersistentProperties

trait VersioningSupport extends PersistentProperties {
  def version: Opt[Int] =
    withSession { implicit session =>
      persistence.intProperty("databaseVersion")
    }

  private var upgrades = Map.empty[Int, UpgradableVersion]

  def register(upgrade: UpgradableVersion): Unit = synchronized {
    upgrades += upgrade.version -> upgrade
  }

  /**
   * Upgrades the database to the latest version iteratively. This utilizes the
   * registered versions compared against the current version in the database
   * and iterates over all unapplied versions upgrading to the latest.
   */
  def upgrade(): Unit = synchronized {
    info("Checking for Database Upgrades...")

    withSession { implicit session =>
      val latestVersion = upgrades.keys.toList match {
        case Nil => 0
        case keys => keys.max
      }

      val newDatabase = version.get.isEmpty && persistence.get("databaseVersion").isEmpty

      if (latestVersion == 0) {
        // Make sure the value is created in the database
        info(s"New database created. Setting version to latest (version $latestVersion) without running upgrades.")
        version := latestVersion // New database created, we don't have to run upgrades
      } else {
        info(s"Current Version: ${version.get}, Latest Version: $latestVersion")
        (version.get.getOrElse(0) until latestVersion).foreach { currentVersion =>
          transaction { implicit session =>
            val nextVersion = currentVersion + 1
            info(s"Upgrading from version $currentVersion to $nextVersion...")
            val upgrade = upgrades.getOrElse(nextVersion,
              throw new RuntimeException(s"No version registered for $nextVersion."))
            if (newDatabase && !upgrade.runOnNewDatabase) {
              version := nextVersion
              info(s"Skipping version $currentVersion to $nextVersion because it's a new database.")
            } else {
              val elapsed = Time.elapsed {
                upgrade.upgrade(session)
              }
              version := nextVersion
              info(s"$nextVersion upgrade finished successfully in $elapsed seconds.")
            }
          }
        }
      }
    }

    upgrades = Map.empty  // Clear the map so upgrades can be released
  }
}