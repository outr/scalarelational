package org.scalarelational.h2.existing

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey}
import org.scalarelational.existing.ExistingQuery
import org.scalarelational.h2.H2Datastore
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

import scala.language.reflectiveCalls

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ExistingQuerySpec extends WordSpec with Matchers {
  import TestDatastore._

  "ExistingQuery" should {
    val queryString = "SELECT id, name, language FROM users WHERE id = ?"
    val existingQuery = new ExistingQuery[ExistingResult](TestDatastore, queryString)
    "create the database" in {
      session {
        create(users)
      }
    }
    "insert some records" in {
      session {
        insert(users.name("Adam"), users.language("English")).
           and(users.name("Victor"), users.language("Russian")).result
      }
    }
    "query back a specific result" in {
      session {
        val results = existingQuery.query(List(2)).toList
        results.length should equal(1)
        val result = results.head
        result.id should equal(2)
        result.name should equal("Victor")
        result.language should equal("Russian")
      }
    }
  }
}

case class ExistingResult(id: Int, name: String, language: String)

object TestDatastore extends H2Datastore {
  val users = new Table("users") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val language = column[String]("language")
  }
}