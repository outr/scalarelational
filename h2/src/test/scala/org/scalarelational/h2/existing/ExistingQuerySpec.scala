package org.scalarelational.h2.existing

import java.sql.ResultSet

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey}
import org.scalarelational.datatype.DataTypes
import org.scalarelational.existing.{ExistingQuery, NamedArgument}
import org.scalarelational.h2.H2Datastore
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

import scala.language.reflectiveCalls


class ExistingQuerySpec extends WordSpec with Matchers {
  import TestDatastore._

  "ExistingQuery" should {
    val queryString1 = "SELECT id, name, language FROM users WHERE id = ?"
    val queryString2 = "SELECT id, name, language FROM users WHERE id = :id"
    val converter = (rs: ResultSet) => {
      val id = rs.getInt("id")
      val name = rs.getString("name")
      val language = rs.getString("language")
      ExistingResult(id, name, language)
    }
    val existingQuery1 = new ExistingQuery[ExistingResult](TestDatastore, queryString1, converter)
    val existingQuery2 = new ExistingQuery[ExistingResult](TestDatastore, queryString2, converter)
    "create the database" in {
      withSession { implicit session =>
        create(users)
      }
    }
    "insert some records" in {
      withSession { implicit session =>
        insert(users.name("Adam"), users.language("English")).
           and(users.name("Victor"), users.language("Russian")).result
      }
    }
    "query back a specific result" in {
      withSession { implicit session =>
        val results = existingQuery1.query(List(DataTypes.IntType.typed(2))).toList
        results.length should equal(1)
        val result = results.head
        result.id should equal(2)
        result.name should equal("Victor")
        result.language should equal("Russian")
      }
    }
    "query back a specific result with a NamedArgument" in {
      withSession { implicit session =>
        val results = existingQuery2.query(List(new NamedArgument("id", "1"))).toList
        results.length should equal(1)
        val result = results.head
        result.id should equal(1)
        result.name should equal("Adam")
        result.language should equal("English")
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