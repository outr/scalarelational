package org.scalarelational.h2.modular

import java.sql.Timestamp

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.h2.H2Datastore
import org.scalarelational.model.{ModularSupport, Table}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
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
        create(users)
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
        val q = select(id, name, age, modified) from users
        q.result.converted.one should equal((1, "John Doe", 21, None))
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
    "add a special 'modified' handler" in {
      users.handlers.inserting.on {
        case insert =>
          insert.add(users.modified(Some(new Timestamp(System.currentTimeMillis()))))
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
    "query back the record expecting a modified date" in {
      session {
        import users._
        querying should equal(1)
        queried should equal(1)
        val q = select(id, name, age, modified) from users
        val result = q.result.converted.one
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
  }
}

object ModularDatastore extends H2Datastore {
  object users extends Table("users") with ModularSupport {
    val name = column[String]("name", Unique)
    val age = column[Int]("age")
    val modified = column[Option[Timestamp]]("modified")
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
  }
}