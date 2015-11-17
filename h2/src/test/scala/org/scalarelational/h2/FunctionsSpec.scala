package org.scalarelational.h2

import java.sql.Connection

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}


class FunctionsSpec extends WordSpec with Matchers {
  import FunctionsDatastore._

  "FunctionsTest" should {
    "create the tables" in {
      withSession {
        create(users)
      }
    }
    "invoke the createUser function" in {
      withSession {
        createUser("John Doe", 21)
      }
    }
    "get the created user out via byName function" in {
      withSession {
        val results = byName("John Doe")
        try {
          results.next() should equal(true)
          results.getString("name") should equal("John Doe")
        } finally {
          results.close()
        }
      }
    }
    "query the created user out" in {
      withSession {
        val query = select(users.*) from users
        val results = query.result.toList
        results.size should equal(1)
        val result = results.head
        result(users.name) should equal("John Doe")
        result(users.age) should equal(21)
      }
    }
  }
}

object FunctionsDatastore extends H2Datastore(mode = H2Memory("functions")) {
  object users extends Table("users") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", Unique)
    val age = column[Int]("age")
  }

  val createUser = function(Functions, "createUser") {
    case f => (name: String, age: Int) => f.call(name, age)
  }
  val byName = function(Functions, "byName") {
    case f => (name: String) => f.query(name)
  }
}

object Functions {
  def createUser(c: Connection, name: String, age: Int): Unit = {
    val s = c.prepareCall("INSERT INTO Users (name, age) VALUES (?, ?)")
    s.setString(1, name)
    s.setInt(2, age)
    s.execute()
  }

  def byName(c: Connection, name: String) = {
    val s = c.prepareCall("SELECT * FROM Users WHERE name = ?")
    s.setString(1, name)
    s.executeQuery()
  }
}