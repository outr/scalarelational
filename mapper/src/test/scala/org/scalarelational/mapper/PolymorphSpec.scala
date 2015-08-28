package org.scalarelational.mapper

import java.sql.Types

import org.scalarelational.column.ColumnLike
import org.scalarelational.column.property.{AutoIncrement, Polymorphic, PrimaryKey}
import org.scalarelational.datatype.{DataType, SQLConversion, SQLType}
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
class PolymorphSpec extends WordSpec with Matchers {
  import PolymorphDatastore._

  val insertUsers = Seq(
    UserGuest("guest"),
    UserAdmin("admin", canDelete = true)
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
        insertUsers.zipWithIndex.foreach {
          case (usr, index) => {
            val result = usr.insert.result
            result.id should equal (index + 1)
          }
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
        insertContent.zipWithIndex.foreach {
          case (c, index) => {
            val result = c.insert.result
            result.id should equal (index + 1)
          }
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

trait User extends BaseEntity[User] {
  def name: String
  def id: Option[Int]
  def withoutId: User
}

case class UserGuest(name: String, id: Option[Int] = None)
  extends User with Entity[UserGuest] {
  def columns = mapTo[UserGuest](PolymorphDatastore.users)

  val isGuest = true
  def withoutId = copy(id = None)
}

case class UserAdmin(name: String, canDelete: Boolean, id: Option[Int] = None)
  extends User with Entity[UserAdmin] {
  def columns = mapTo[UserAdmin](PolymorphDatastore.users)

  val isGuest = false
  def withoutId = copy(id = None)
}

// ---

trait Content extends BaseEntity[Content] {
  def id: Option[Int]
  def withoutId: Content
}

case class ContentString(string: String, id: Option[Int] = None)
  extends Content with Entity[ContentString] {
  def columns = mapTo[ContentString](PolymorphDatastore.content)

  val isString = true
  def withoutId = copy(id = None)
}

case class ContentList(entries: List[String], id: Option[Int] = None)
  extends Content with Entity[ContentList] {
  def columns = mapTo[ContentList](PolymorphDatastore.content)

  val isString = false
  def withoutId = copy(id = None)
}

// ---

object PolymorphDatastore extends H2Datastore(mode = H2Memory("polymorph_test")) {
  object users extends MappedTable[User]("users") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val canDelete = column[Boolean]("canDelete", Polymorphic)
    val isGuest = column[Boolean]("isGuest")

    override def query = q.to[User]
  }

  object content extends MappedTable[Content]("content") {
    object ListConverter extends SQLConversion[List[String], String] {
      override def toSQL(column: ColumnLike[List[String], String], value: List[String]): String = value.mkString("|")
      override def fromSQL(column: ColumnLike[List[String], String], value: String): List[String] = value.split('|').toList
    }
    implicit def listDataType = new DataType[List[String], String](Types.VARCHAR, SQLType("VARCHAR(1024)"), ListConverter)

    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val string = column[String]("string", Polymorphic)
    val entries = column[List[String], String]("entries", Polymorphic)
    val isString = column[Boolean]("isString")

    override def query = q.to[Content]
  }
}
