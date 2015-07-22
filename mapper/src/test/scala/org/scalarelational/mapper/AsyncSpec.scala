package org.scalarelational.mapper

import java.util.concurrent.atomic.AtomicInteger

import org.powerscala.concurrent.Time
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.table.Table
import org.scalarelational.column.property.{PrimaryKey, AutoIncrement}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class AsyncSpec extends WordSpec with Matchers {
  import AsyncDatastore._

  import scala.concurrent.ExecutionContext.Implicits.global

  "Async" should {
    "create tables" in {
      session {
        create(users)
      }
    }
    "insert a bunch of values asynchronously" in {
      val running = new AtomicInteger(0)

      session {
        (0 until 100).foreach {
          case index => {
            running.incrementAndGet()
            insert(users.name(s"User $index"), users.age(index)).async.onSuccess {
              case v => running.decrementAndGet()
            }
          }
        }
        Time.waitFor(5.0, 0.5) {
          running.get() == 0
        }
      }
    }
    "persist a bunch of values asynchronously" in {
      val running = new AtomicInteger(0)

      session {
        (0 until 100).foreach {
          case index => {
            running.incrementAndGet()
            users.insert(AsyncUser(s"User $index", index)).async.onSuccess {
              case v => running.decrementAndGet()
            }
          }
        }
        Time.waitFor(5.0, 0.5) {
          running.get() == 0
        }
      }
    }
  }
}

case class AsyncUser(name: String, age: Int, id: Option[Int] = None)

object AsyncDatastore extends H2Datastore(mode = H2Memory("async_test")) {
  object users extends Table("users") {
    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val age = column[Int]("age")
  }
}