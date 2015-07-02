package org.scalarelational.mapper

import org.scalarelational.column.property.{AutoIncrement, NotNull, PrimaryKey}
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.model.Table
import org.scalarelational.result.QueryResult
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */

class PolymorphSpec extends WordSpec with Matchers {
  import PolymorphDatastore._

  val usr1 = UserGuest("guest")
  val usr2 = UserAdmin("admin", true)
  val insertUsers = Seq(usr1, usr2)

  "Async" should {
    "create tables" in {
      session {
        create(users)
      }
    }
    "insert users" in {
      session {
        // insertUsers.foreach(users.persist(_).result)
        users.persist(usr1).result
        users.persist(usr2).result
      }
    }
    "query users" in {
      import users._

      session {
        val query = select (*) from users
        println(query.result.toList)
        val x = query.asCase { (res: QueryResult[User]) =>
          val cls =
            if (res(users.isGuest)) classOf[UserGuest]
            else classOf[UserAdmin]
          cls.asInstanceOf[Class[User]]
        }
        insertUsers should equal (x.result.toList)
      }
    }
  }
}

trait User {
  def name: String
  def id: Option[Int]
}
case class UserGuest(name: String, id: Option[Int] = None) extends User {
  val isGuest = true
}
case class UserAdmin(name: String, canDelete: Boolean, id: Option[Int] = None) extends User {
  val isGuest = false
}

object PolymorphDatastore extends H2Datastore(mode = H2Memory("polymorph_test")) {
  object users extends Table("users") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", NotNull)
    val canDelete = column[Boolean]("canDelete")
    val isGuest = column[Boolean]("isGuest", NotNull)
  }
}
