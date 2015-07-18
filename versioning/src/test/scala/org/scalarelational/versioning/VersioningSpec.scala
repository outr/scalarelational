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

  "Versioning" should {
    "create the database" in {
      session {
        create(test, persistentProperties)
      }
    }
    "have version at 0" in {
      session {
        version() should equal(0)
        persistence.get("databaseVersion") should equal(None)
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
    "have version at 1" in {
      session {
        version() should equal(1)
        persistence.get("databaseVersion") should equal(Some("1"))
      }
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
    "have version at 3" in {
      session {
        Upgrade2.invoked should equal(true)
        Upgrade3.invoked should equal(true)
        version() should equal(3)
        persistence.get("databaseVersion") should equal(Some("3"))
      }
    }
    "register a fourth upgrade" in {
      register(Upgrade4)
    }
    "have only two tables in the datastore" in {
      session {
        jdbcTables should equal(Set("TEST", "PERSISTENT_PROPERTIES"))
      }
    }
    "run fourth upgrade" in {
      session {
        upgrade()
      }
    }
    "have version at 4" in {
      session {
        version() should equal(4)
        persistence.get("databaseVersion") should equal(Some("4"))
        jdbcTables should equal(Set("TEST", "TEST2", "PERSISTENT_PROPERTIES"))
        jdbcColumns("TEST2") should equal(Set("ID", "NAME", "AGE"))
      }
    }
    "register a fifth upgrade" in {
      register(Upgrade5)
    }
    "run fifth upgrade" in {
      session {
        upgrade()
      }
    }
    "have version at 5" in {
      session {
        version() should equal(5)
        persistence.get("databaseVersion") should equal(Some("5"))
        jdbcTables should equal(Set("TEST", "TEST2", "PERSISTENT_PROPERTIES"))
        jdbcColumns("TEST2") should equal(Set("ID", "AGE"))
      }
    }
    "register a sixth upgrade" in {
      register(Upgrade6)
    }
    "run sixth upgrade" in {
      session {
        upgrade()
      }
    }
    "have version at 6" in {
      session {
        version() should equal(6)
        persistence.get("databaseVersion") should equal(Some("6"))
        jdbcTables should equal(Set("TEST", "TEST2", "PERSISTENT_PROPERTIES"))
        jdbcColumns("TEST2") should equal(Set("ID", "YEARSOLD"))
      }
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

object Upgrade4 extends UpgradableVersion {
  override def version = 4
  override def upgrade() = {
    import VersioningDatastore._

    createTable("test2").
      and(createColumn[Int]("test2", "id", PrimaryKey, AutoIncrement)).
      and(createColumn[String]("test2", "name")).
      and(createColumn[Option[Int]]("test2", "age")).result
  }
}

object Upgrade5 extends UpgradableVersion {
  override def version = 5
  override def upgrade() = {
    import VersioningDatastore._

    dropColumn("test2", "name").result
  }
}

object Upgrade6 extends UpgradableVersion {
  override def version = 6
  override def upgrade() = {
    import VersioningDatastore._

    renameColumn("test2", "age", "yearsOld").result
  }
}

object VersioningDatastore extends H2Datastore(mode = H2Memory("versioning")) with VersioningSupport {
  object test extends Table("test") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", Unique)
  }
}