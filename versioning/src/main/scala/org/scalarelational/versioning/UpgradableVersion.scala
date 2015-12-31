package org.scalarelational.versioning

import org.scalarelational.Session

/**
 * UpgradableVersions are registered to a Datastore that mixes-in VersioningSupport. An UpgradableVersion represents a
 * specific version number of the database that can be upgraded to. By default the database starts at zero an
 * UpgradableVersion must be registered for each incrementing version. The responsibility of each UpgradableVersion is
 * to upgrade from one version behind to this version.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait UpgradableVersion {
  /**
    * The version this upgrade will apply. The first upgrade version should start at 1.
    */
  def version: Int

  /**
    * If true, will run even if it's a new database. If false, this upgrade is skipped upon database creation.
    *
    * Defaults to false
    */
  def runOnNewDatabase: Boolean = false

  /**
    * Runs the upgrade
    */
  def upgrade(implicit session: Session): Unit
}
