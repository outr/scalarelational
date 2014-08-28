package com.outr.query.h2

import java.sql.Connection

import com.outr.query.Table
import com.outr.query.column.property._
import com.outr.query.h2.Names._
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class FunctionsSpec extends WordSpec with Matchers {
  "FunctionsTest" should {
    "create the tables" in {
      FunctionsDatastore.create()
    }
    "invoke the createUser function" in {
      FunctionsDatastore.createUser("John Doe", 21)
    }
    "get the created user out via byName function" in {
      val results = FunctionsDatastore.byName("John Doe")
      try {
        results.next() should equal(true)
        results.getString("name") should equal("John Doe")
      } finally {
        results.close()
      }
    }
    "query the created user out" in {
      val query = FunctionsDatastore.select(Users.*) from Users
      val results = FunctionsDatastore.exec(query).toList
      results.size should equal(1)
      val result = results.head
      result(Users.name) should equal("John Doe")
      result(Users.age) should equal(21)
    }
  }
}

object FunctionsDatastore extends H2Datastore(mode = H2Memory("functions")) {
  def users = Users

  val createUser = function(Functions, "createUser") {
    case f => (name: String, age: Int) => f.call(name, age)
  }
  val byName = function(Functions, "byName") {
    case f => (name: String) => f.query(name)
  }
}

object Users extends Table(FunctionsDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", NotNull, Unique)
  val age = column[Int]("age", NotNull)
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