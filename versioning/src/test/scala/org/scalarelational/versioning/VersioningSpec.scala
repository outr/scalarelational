package org.scalarelational.versioning

import org.scalarelational.column.property.{Unique, AutoIncrement, PrimaryKey}
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.model.Table
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class VersioningSpec extends WordSpec with Matchers {
  import VersioningDatastore._
  import test._

  "Versioning" should {
    "create the database" in {
      session {
        create(test, persistentProperties)
      }
    }
    "version should be 0" in {
      session {
        version() should equal(0)
      }
    }
    "register an upgrade" in {
      session {
        register(Upgrade1)
      }
    }
    "run upgrades but automatically update version" in {
      session {
        upgrade()
      }
    }
    "version should be 1" in {
      version() should equal(1)
    }
    "register more upgrades" in {
      register(Upgrade2)
      register(Upgrade3)
    }
    "run upgrades" in {
      session {
        upgrade()
      }
    }
    "version should be 3" in {
      Upgrade2.invoked should equal(true)
      Upgrade3.invoked should equal(true)
      version() should equal(3)
    }
  }
}

object Upgrade1 extends UpgradableVersion {
  override def version = 1

  override def upgrade() = throw new RuntimeException("This should never be invoked.")
}

object Upgrade2 extends UpgradableVersion {
  var invoked = false

  override def version = 2

  override def upgrade() = {
    invoked = true
  }
}

object Upgrade3 extends UpgradableVersion {
  var invoked = false

  override def version = 3

  override def upgrade() = {
    invoked = true
  }
}

object VersioningDatastore extends H2Datastore(mode = H2Memory("versioning")) with VersioningSupport {
  object test extends Table("test") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", Unique)
  }
}