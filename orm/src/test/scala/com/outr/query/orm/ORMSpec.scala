package com.outr.query.orm

import org.specs2.mutable._
import com.outr.query.h2.{H2Memory, H2Datastore}
import com.outr.query.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMSpec extends Specification {
  import TestDatastore._

  "Person" should {
    "create the tables" in {
      create() must not(throwA[Throwable])
    }
    "insert 'John Doe' into the table" in {
      val john = Person("John Doe")
      val updated = person.insert(john)
      updated.id mustEqual Some(1)
    }
    "insert 'Jane Doe' into the table" in {
      val jane = Person("Jane Doe")
      val updated = person.insert(jane)
      updated.id mustEqual Some(2)
    }
    "query back all records" in {
      val results = person.query(select(personTable.*) from personTable).toList
      results must have size 2
    }
    "query back 'John Doe' with only 'name'" in {
      val results = person.query(select(personTable.name) from personTable where personTable.name === "John Doe").toList
      results must have size 1
      val john = results.head
      john.id mustEqual None
      john.name mustEqual "John Doe"
    }
    "query back 'Jane Doe' with all fields" in {
      val results = person.query(select(personTable.*) from personTable where personTable.name === "Jane Doe").toList
      results must have size 1
      val jane = results.head
      jane.id mustEqual Some(2)
      jane.name mustEqual "Jane Doe"
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val personTable = new Table("person") {
    val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
    val name = Column[String]("name", unique = true)
    val date = Column[Long]("date")
  }
  val person = orm[Person](personTable)
}

case class Person(name: String, date: Long = System.currentTimeMillis(), id: Option[Int] = None)