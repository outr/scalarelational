package org.scalarelational.mapper

import org.scalarelational.column.property.{Polymorphic, AutoIncrement, PrimaryKey}
import org.scalarelational.datatype.{StringDataType, DataType}
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.model.{ColumnLike, Table}
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

  val insertContent = Seq(
    ContentString("hello"),
    ContentList(List("a", "b", "c"))
  )

  "Users" should {
    "create tables" in {
      session {
        create(users)
      }
    }
    "insert users" in {
      session {
        insertUsers.foreach { usr =>
          val result = users.persist(usr).result
          (result != usr && result.withoutId == usr) should equal (true)
        }
      }
    }
    "query users" in {
      session {
        val query = users.q from users
        val x = query.asCase[User] { row =>
          if (row(users.isGuest)) classOf[UserGuest]
          else classOf[UserAdmin]
        }
        insertUsers should equal (x.result.converted.toList.map(_.withoutId))
      }
    }
  }

  "Content" should {
    "create tables" in {
      session {
        create(content)
      }
    }
    "insert content" in {
      session {
        insertContent.foreach { c =>
          val result = content.persist(c).result
          (result != c && result.withoutId == c) should equal (true)
        }
      }
    }
    "query content" in {
      session {
        val query = content.q from content
        val x = query.asCase[Content] { row =>
          if (row(content.isString)) classOf[ContentString]
          else classOf[ContentList]
        }
        insertContent should equal (x.result.converted.toList.map(_.withoutId))
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

// ---

trait Content {
  def id: Option[Int]
  def withoutId: Content
}

case class ContentString(string: String, id: Option[Int] = None) extends Content {
  val isString = true
  def withoutId = copy(id = None)
}

case class ContentList(entries: List[String], id: Option[Int] = None) extends Content {
  val isString = false
  def withoutId = copy(id = None)
}

// ---

object PolymorphDatastore extends H2Datastore(mode = H2Memory("polymorph_test")) {
  object users extends Table("users") {
    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val canDelete = column[Boolean]("canDelete", Polymorphic)
    val isGuest = column[Boolean]("isGuest")
  }

  object content extends Table("content") {
    implicit val listStringConverter = new DataType[List[String]] {
      def sqlType(column: ColumnLike[_]) = "VARCHAR(1024)"
      def toSQLType(column: ColumnLike[_], value: List[String]) = value.mkString("|")
      def fromSQLType(column: ColumnLike[_], value: Any) =
        value.asInstanceOf[String].split('|').toList
    }

    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val string = column[String]("string", Polymorphic)
    val entries = column[List[String]]("entries", Polymorphic)
    val isString = column[Boolean]("isString")
  }
}
