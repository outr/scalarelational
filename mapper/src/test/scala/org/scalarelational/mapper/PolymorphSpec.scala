package org.scalarelational.mapper

import org.scalarelational.column.property.{AutoIncrement, NotNull, PrimaryKey}
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.model.Table
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */

class PolymorphSpec extends WordSpec with Matchers {
  import PolymorphDatastore._

  val insertUsers = Seq(
    UserGuest("guest"),
    UserAdmin("admin", true)
  )

  "Async" should {
    "create tables" in {
      session {
        create(users)
      }
    }
    "insert users" in {
      session {
        insertUsers.foreach(users.persist(_).result)
      }
    }
    "query users" in {
      import users._

      session {
        val query = select (*) from users
        val x = query.asCase[User] { row =>
          if (row(users.isGuest)) classOf[UserGuest]
          else classOf[UserAdmin]
        }
        insertUsers should equal (x.result.converted.toList.map(_.withoutId))
      }
    }
  }
}

trait User {
  def name: String
  def id: Option[Int]
  def withoutId: User
}
case class UserGuest(name: String, id: Option[Int] = None) extends User {
  val isGuest = true
  def withoutId = copy(id = None)
}
case class UserAdmin(name: String, canDelete: Boolean, id: Option[Int] = None) extends User {
  val isGuest = false
  def withoutId = copy(id = None)
}

object PolymorphDatastore extends H2Datastore(mode = H2Memory("polymorph_test")) {
  object users extends Table("users") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", NotNull)
    val canDelete = column[Boolean]("canDelete")
    val isGuest = column[Boolean]("isGuest", NotNull)
  }
}
