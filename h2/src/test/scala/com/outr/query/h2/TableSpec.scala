package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification {
  import TestDatastore._

  "TestTable" should {
    "have two columns" in {
      test.columns must have size 3
    }
    "verify the create table String is correct" in {
      val sql = TestDatastore.createTableSQL(ifNotExist = true, test)
      sql mustEqual "CREATE TABLE IF NOT EXISTS test(id INTEGER AUTO_INCREMENT, name VARCHAR(2147483647) UNIQUE, date BIGINT, PRIMARY KEY(id))"
    }
    "create the table" in {
      create() must not(throwA[Throwable])
    }
    "insert a record" in {
      val id = insert(test.name("John Doe")).toList.head
      id mustEqual 1
    }
    "create a simple query" in {
      val q = select(test.id, test.name).from(test)
      q.columns must have size 2
    }
    "query the record back out" in {
      val results = exec(select(test.id, test.name).from(test)).toList
      results must have size 1
      val john = results.head
      john(test.id) mustEqual 1
      john(test.name) mustEqual "John Doe"
    }
    "insert another record" in {
      insert(test.name("Jane Doe")) must not(throwA[Throwable])
    }
    "query the record back by name" in {
      val results = exec(select(test.id, test.name).from(test).where(test.name === "Jane Doe")).toList
      results must have size 1
      val jane = results.head
      jane(test.id) mustEqual 2
      jane(test.name) mustEqual "Jane Doe"
    }
//    "query with multiple where clauses" in {
//      val query = select (test.id, test.name) from test where (test.name === "Jane Doe" or test.name === "John Doe") and test.id > 0
//      val results = exec(query).toList
//      results must have size 2
//    }
    // TODO: delete a record
    // TODO: update a record
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val test = new Table("test") {
    val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
    val name = Column[String]("name", unique = true)
    val date = Column[Long]("date")
  }
}