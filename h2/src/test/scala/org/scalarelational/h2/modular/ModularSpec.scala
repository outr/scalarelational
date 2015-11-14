package org.scalarelational.h2.modular

import java.sql.Timestamp

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.h2.H2Datastore
import org.scalarelational.model.ModularSupport
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}


class ModularSpec extends WordSpec with Matchers {
  import ModularDatastore._

  private var inserting = 0
  private var merging = 0
  private var updating = 0
  private var deleting = 0
  private var querying = 0
  private var inserted = 0
  private var merged = 0
  private var updated = 0
  private var deleted = 0
  private var queried = 0

  "ModularSpec" should {
    "create the database" in {
      session {
        create(users, users2)
      }
    }
    "connect handlers" in {
      users.handlers.inserting.on {
        case evt => {
          inserting += 1
          evt
        }
      }
      users.handlers.merging.on {
        case evt => {
          merging += 1
          evt
        }
      }
      users.handlers.updating.on {
        case evt => {
          updating += 1
          evt
        }
      }
      users.handlers.deleting.on {
        case evt => {
          deleting += 1
          evt
        }
      }
      users.handlers.querying.on {
        case evt => {
          querying += 1
          evt
        }
      }
      users.handlers.inserted.on {
        case evt => {
          inserted += 1
        }
      }
      users.handlers.merged.on {
        case evt => {
          merged += 1
        }
      }
      users.handlers.updated.on {
        case evt => {
          updated += 1
        }
      }
      users.handlers.deleted.on {
        case evt => {
          deleted += 1
        }
      }
      users.handlers.queried.on {
        case evt => {
          queried += 1
        }
      }
    }
    "insert a record" in {
      session {
        import users._
        inserting should equal(0)
        inserted should equal(0)
        insert(name("John Doe"), age(21)).result
        inserting should equal(1)
        inserted should equal(1)
      }
    }
    "query a record" in {
      session {
        import users._
        querying should equal(0)
        queried should equal(0)
        val q = select(id, name, age, created) from users
        q.converted.one should equal((1, "John Doe", 21, None))
        querying should equal(1)
        queried should equal(1)
      }
    }
    "delete a record" in {
      session {
        import users._
        deleting should equal(0)
        deleted should equal(0)
        (delete(users) where (id === 1)).result
        deleting should equal(1)
        deleted should equal(1)
      }
    }
    "add a special 'created' handler" in {
      users.handlers.inserting.on {
        case insert =>
          insert.add(users.created(Some(new Timestamp(System.currentTimeMillis()))))
      }
    }
    "insert a second record" in {
      session {
        import users._
        inserting should equal(1)
        inserted should equal(1)
        insert(name("Jane Doe"), age(20)).result
        inserting should equal(2)
        inserted should equal(2)
      }
    }
    "query back the record expecting a created date" in {
      session {
        import users._
        querying should equal(1)
        queried should equal(1)
        val q = select(id, name, age, created) from users
        val result = q.converted.one
        result._1 should equal(2)
        result._2 should equal("Jane Doe")
        result._3 should equal(20)
        result._4 should not equal null
        result._4.get.getTime should be > (System.currentTimeMillis() - 1000L)
        result._4.get.getTime should be <= System.currentTimeMillis()
        querying should equal(2)
        queried should equal(2)
      }
    }
    "add a special 'modified' handler" in {
      /* We will compare `modified` in the next test to `created`. Ensure that
       * `modified` cannot have the same timestamp.
       */
      val DummyValue = 1

      users.handlers.updating.on {
        case update =>
          update.add(users.modified(Some(new Timestamp(System.currentTimeMillis() + DummyValue))))
      }
    }
    "updating second record" in {
      session {
        import users._
        (update(name("updated")) where id === 2).result
      }
    }
    "query back the record expecting a modified date" in {
      session {
        import users._
        val q = select (id, name, created, modified) from users where id === 2
        val result = q.converted.one
        result._2 should equal("updated")
        result._4.get.after(result._3.get) should be (true)
      }
    }
    "insert and update record using mixin" in {
      session {
        import users2._
        insert(name("Jane Doe"), age(20)).result

        val q = select (created, modified) from users2 where id === 1
        val result = q.converted.one

        (update(name("updated")) where id === 1).result
        val q2 = select (created, modified) from users2 where id === 1
        val result2 = q2.converted.one
        result2._2.after(result2._1) should be (true)
        result2._2.after(result._1) should be (true)
      }
    }
  }
}

object ModularDatastore extends H2Datastore {
  object users extends Table("users") with ModularSupport {
    val name = column[String]("name", Unique)
    val age = column[Int]("age")
    val created = column[Option[Timestamp], Timestamp]("created")
    val modified = column[Option[Timestamp], Timestamp]("modified")
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
  }

  // If more than one table should be equipped with `created` and `modified`
  // fields that get updated automatically, then this mixin can be used.
  trait Timestamps extends ModularSupport { this: Table =>
    val created  = column[Timestamp]("created")
    val modified = column[Timestamp]("modified")
    val DummyValue = 1

    handlers.inserting.on { insert =>
      insert
        .add(created(new Timestamp(System.currentTimeMillis())))
        .add(modified(new Timestamp(System.currentTimeMillis())))
    }

    handlers.updating.on { update =>
      update.add(modified(new Timestamp(System.currentTimeMillis() + DummyValue)))
    }
  }

  object users2 extends Table("users2") with Timestamps {
    val name = column[String]("name", Unique)
    val age = column[Int]("age")
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
  }
}
