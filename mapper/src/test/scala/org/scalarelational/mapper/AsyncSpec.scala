package org.scalarelational.mapper

import java.util.concurrent.atomic.AtomicInteger

import org.scalarelational.SelectExpression
import org.scalarelational.column.ColumnValue
import org.scalarelational.column.property.{AutoIncrement, PrimaryKey}
import org.scalarelational.h2.{H2Database, H2Memory}
import org.scalarelational.instruction.Query
import org.scalarelational.util.Time
import org.scalatest.{Matchers, WordSpec}

class AsyncSpec extends WordSpec with Matchers {
  import AsyncDatabase._

  import scala.concurrent.ExecutionContext.Implicits.global

  "Async" should {
    "create tables" in {
      withSession { implicit session =>
        create(users)
      }
    }
    "insert a bunch of values asynchronously" in {
      val running = new AtomicInteger(0)

      withSession { implicit session =>
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

      withSession { implicit session =>
        (0 until 100).foreach {
          case index => {
            running.incrementAndGet()
            AsyncUser(s"User $index", index).insert.async.onSuccess {
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
  extends Entity[AsyncUser] {
  def columns: List[ColumnValue[Any, Any]] = mapTo[AsyncUser](AsyncDatabase.users)
}

object AsyncDatabase extends H2Database(mode = H2Memory("async_test")) {
  object users extends MappedTable[AsyncUser]("users") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val age = column[Int]("age")

    override def query: Query[scala.Vector[SelectExpression[_]], AsyncUser] = q.to[AsyncUser](this)
  }
}