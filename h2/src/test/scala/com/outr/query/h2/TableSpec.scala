package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification {
  import TestDatastore._

  var acmeId: Int = _
  var superiorId: Int = _
  var highGroundId: Int = _

  "test" should {
    "have two columns" in {
      test.columns must have size 3
    }
    "have the proper table-to-table relationships set" in {
      test.one2One must have size 0
      test.one2Many must have size 0
      test.many2One must have size 0
      test.many2Many must have size 0
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
    "update 'John Doe' to 'Joe Doe'" in {
      val updated = exec(update(test.name("Joe Doe")) where(test.name === "John Doe"))
      updated mustEqual 1
    }
    "verify that 'John Doe' no longer exists" in {
      val results = exec(select(test.name).from(test).where(test.name === "John Doe")).toList
      results must have size 0
    }
    "verify that 'Joe Doe' does exist" in {
      val results = exec(select(test.name).from(test).where(test.name === "Joe Doe")).toList
      results must have size 1
    }
    "verify that 'Jane Doe' wasn't modified" in {
      val results = exec(select(test.name).from(test).where(test.name === "Jane Doe")).toList
      results must have size 1
    }
    "delete 'Joe Doe' from the database" in {
      val deleted = exec(delete(test) where test.name === "Joe Doe")
      deleted mustEqual 1
    }
    "verify there is just one record left in the database" in {
      val results = exec(select(test.id, test.name).from(test)).toList
      results must have size 1
      val jane = results.head
      jane(test.id) mustEqual 2
      jane(test.name) mustEqual "Jane Doe"
    }
    "delete everything from the database" in {
      val deleted = exec(delete(test))
      deleted mustEqual 1
    }
    "verify there are no records left in the database" in {
      val results = exec(select(test.id, test.name).from(test)).toList
      results must have size 0
    }
  }
  "suppliers" should {
    import suppliers._
    "have the proper table-to-table relationships set" in {
      suppliers.one2One must have size 0
      suppliers.one2Many must have size 0
      suppliers.many2One must have size 1
      suppliers.many2Many must have size 0
    }
    "insert three suppliers" in {
      acmeId = insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).get
      superiorId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).get
      highGroundId = insert(name("The High Ground"), street("100 Coffee Lane"), city("Meadows"), state("CA"), zip("93966")).get
      acmeId mustNotEqual 0
      superiorId mustNotEqual 0
      highGroundId mustNotEqual 0
    }
  }
  "coffees" should {
    import coffees._
    "have the proper table-to-table relationships set" in {
      coffees.one2One must have size 0
      coffees.one2Many must have size 1
      coffees.many2One must have size 0
      coffees.many2Many must have size 0
    }
    "insert five coffees" in {
      insert(name("Colombian"), supID(acmeId), price(7.99), sales(0), total(0)) must not(throwA[Throwable])
      insert(name("French Roast"), supID(superiorId), price(8.99), sales(0), total(0)) must not(throwA[Throwable])
      insert(name("Espresso"), supID(highGroundId), price(9.99), sales(0), total(0)) must not(throwA[Throwable])
      insert(name("Colombian Decaf"), supID(acmeId), price(8.99), sales(0), total(0)) must not(throwA[Throwable])
      insert(name("French Roast Decaf"), supID(superiorId), price(9.99), sales(0), total(0)) must not(throwA[Throwable])
    }
    "query five coffees back out" in {
      val results = exec(select(*) from coffees).toList
      results must have size 5
    }
    "query joining suppliers" in {
      val query = select(*) from coffees innerJoin suppliers on suppliers.id === supID
      val results = exec(query).toList
      results must have size 5
    }
  }
  // TODO: joins
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val test = new Table("test") {
    val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
    val name = Column[String]("name", unique = true)
    val date = Column[Long]("date")
  }
  val suppliers = new Table("SUPPLIERS") {
    val id = Column[Int]("SUP_ID", primaryKey = true, autoIncrement = true)
    val name = Column[String]("SUP_NAME")
    val street = Column[String]("STREET")
    val city = Column[String]("CITY")
    val state = Column[String]("STATE")
    val zip = Column[String]("ZIP")
  }
  val coffees = new Table("COFFEES") {
    val name = Column[String]("COF_NAME", primaryKey = true)
    val supID = Column[Int]("SUP_ID", foreignKey = Some(suppliers.id))
    val price = Column[Double]("PRICE")
    val sales = Column[Int]("SALES")
    val total = Column[Int]("TOTAL")
  }
}