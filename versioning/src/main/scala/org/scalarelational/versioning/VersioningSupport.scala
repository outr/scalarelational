package org.scalarelational.versioning

import reactify.Var
import org.scalarelational.extra.PersistentProperties
import org.scalarelational.util.Time

trait VersioningSupport extends PersistentProperties {
  def version: Var[Option[Int]] =
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
    logger.info("Checking for Database Upgrades...")

    withSession { implicit session =>
      val latestVersion = upgrades.keys.toList match {
        case Nil => 0
        case keys => keys.max
      }

      val newDatabase = version.get.isEmpty && persistence.get("databaseVersion").isEmpty

      if (latestVersion == 0) {
        // Make sure the value is created in the database
        logger.info(s"New database created. Setting version to latest (version $latestVersion) without running upgrades.")
        version := Some(latestVersion) // New database created, we don't have to run upgrades
      } else {
        logger.info(s"Current Version: ${version.get}, Latest Version: $latestVersion")
        (version.get.getOrElse(0) until latestVersion).foreach { currentVersion =>
          transaction { implicit session =>
            val nextVersion = currentVersion + 1
            logger.info(s"Upgrading from version $currentVersion to $nextVersion...")
            val upgrade = upgrades.getOrElse(nextVersion,
              throw new RuntimeException(s"No version registered for $nextVersion."))
            if (newDatabase && !upgrade.runOnNewDatabase) {
              version := Some(nextVersion)
              logger.info(s"Skipping version $currentVersion to $nextVersion because it's a new database.")
            } else {
              val elapsed = Time.elapsed {
                upgrade.upgrade(session)
              }
              version := Some(nextVersion)
              logger.info(s"$nextVersion upgrade finished successfully in $elapsed seconds.")
            }
          }
        }
      }
    }

    upgrades = Map.empty  // Clear the map so upgrades can be released
  }
}