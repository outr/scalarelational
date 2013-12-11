package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._
import com.outr.query.property._
import com.outr.query.convert.{StringConverter, ColumnConverter}
import org.specs2.main.ArgumentsShortcuts

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification with ArgumentsShortcuts with ArgumentsArgs {
  addArguments(fullStackTrace)

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
      sql mustEqual "CREATE TABLE IF NOT EXISTS test(id INTEGER AUTO_INCREMENT, name VARCHAR(2147483647) UNIQUE, date BIGINT, PRIMARY KEY(id));"
    }
    "create the table" in {
      create() mustNotEqual null
    }
    "insert a record" in {
      val id = insert(test.name("John Doe")).toList.head
      id mustEqual 1
    }
    "create a simple query" in {
      val q = select(test.id, test.name) from test
      q.expressions must have size 2
    }
    "query the record back out" in {
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
      results must have size 1
      val john = results.head
      john(test.id) mustEqual 1
      john(test.name) mustEqual "John Doe"
    }
    "insert another record" in {
      insert(test.name("Jane Doe")) must not(throwA[Throwable])
    }
    "query the record back by name" in {
      val query = select(test.id, test.name) from test where test.name === "Jane Doe"
      val results = exec(query).toList
      results must have size 1
      val jane = results.head
      jane(test.id) mustEqual 2
      jane(test.name) mustEqual "Jane Doe"
    }
    "query with multiple where clauses" in {
      val query = select (test.id, test.name) from test where (test.name === "Jane Doe" or test.name === "John Doe") and test.id > 0
      val results = exec(query).toList
      results must have size 2
    }
    "update 'John Doe' to 'Joe Doe'" in {
      val updated = exec(update(test.name("Joe Doe")) where(test.name === "John Doe"))
      updated mustEqual 1
    }
    "verify that 'John Doe' no longer exists" in {
      val query = select(test.name) from test where test.name === "John Doe"
      val results = exec(query).toList
      results must have size 0
    }
    "verify that 'Joe Doe' does exist" in {
      val query = select(test.name) from test where test.name === "Joe Doe"
      val results = exec(query).toList
      results must have size 1
    }
    "verify that 'Jane Doe' wasn't modified" in {
      val query = select(test.name) from test where test.name === "Jane Doe"
      val results = exec(query).toList
      results must have size 1
    }
    "delete 'Joe Doe' from the database" in {
      val deleted = exec(delete(test) where test.name === "Joe Doe")
      deleted mustEqual 1
    }
    "verify there is just one record left in the database" in {
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
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
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
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
      val query = select(name, supID, price, sales, total, suppliers.name) from coffees innerJoin suppliers on suppliers.id === supID
      val results = exec(query).toList
      results must have size 5
      val first = results.head
      first.values must have size 6
      first(suppliers.name) mustEqual "Acme, Inc."
    }
    "query the minimum price" in {
      val query = select(price.min) from coffees
      val results = exec(query).toList
      results must have size 1
      val values = results.head
      values.values must have size 1
      val minimumPrice = values(price.min)
      minimumPrice mustEqual 7.99
    }
    "query the count of coffees for Superior Coffee" in {
      val query = select(name.count) from coffees innerJoin suppliers on supID === suppliers.id where suppliers.name === "Superior Coffee"
      val results = exec(query).toList
      results must have size 1
      val values = results.head
      values.values must have size 1
      val count = values(name.count)
      count mustEqual 2
    }
    "query with an inner join aliased" in {
      val s = suppliers as "s"
      val query = select(name, s(suppliers.name)) from coffees innerJoin s on supID === s(suppliers.id)
      val results = exec(query).toList
      results must have size 5
    }
    "query coffees ordered by name and limited to the second result" in {
      val query = select(name) from coffees orderBy(name asc) limit 1 offset 1
      val results = exec(query).toList
      results must have size 1
      val values = results.head
      values.values must have size 1
      val coffeeName = values(name)
      coffeeName mustEqual "Colombian Decaf"
    }
    "query coffees grouped by price" in {
      val query = select(price) from coffees groupBy price orderBy(price asc)
      val results = exec(query).toList
      results must have size 3
      results(0)(price) mustEqual 7.99
      results(1)(price) mustEqual 8.99
      results(2)(price) mustEqual 9.99
    }
  }
  "names" should {
    import names._

    val queryAll = select(*) from names orderBy(name asc)

    "have no records in the table" in {
      val results = exec(queryAll).toList
      results must have size 0
    }
    "merge 'John Doe' for an inserted record" in {
      merge(name, name("John Doe"), age(21))
      val results = exec(queryAll).toList
      results must have size 1
      val result = results.head
      result(name) mustEqual "John Doe"
      result(age) mustEqual 21
    }
    "merge 'John Doe' for an updated record" in {
      merge(name, name("John Doe"), age(25))
      val results = exec(queryAll).toList
      results must have size 1
      val result = results.head
      result(name) mustEqual "John Doe"
      result(age) mustEqual 25
    }
    "merge 'Jane Doe' for an inserted record" in {
      merge(name, name("Jane Doe"), age(22))
      val results = exec(queryAll).toList
      results must have size 2
      val jane = results.head
      jane(name) mustEqual "Jane Doe"
      jane(age) mustEqual 22
      val john = results.tail.head
      john(name) mustEqual "John Doe"
      john(age) mustEqual 25
    }
  }
  "TestCrossReferenceDatastore" should {
    "create the tables successfully" in {
      TestCrossReferenceDatastore.create() must not(throwA[Throwable])
    }
  }
  "TestSpecialTypesDatastore" should {
    import TestSpecialTypesDatastore._

    var id: Int = -1

    "create the tables successfully" in {
      create() must not(throwA[Throwable])
    }
    "insert a List[String] entry" in {
      val idOption = insert(lists.strings(List("One", "Two", "Three")))
      idOption mustNotEqual None
      id = idOption.get
      id mustEqual 1
    }
    "query a List[String] entry" in {
      val query = select(lists.id, lists.strings) from lists
      val results = exec(query).toList
      results must have size 1
      val result = results.head
      result(lists.id) mustEqual id
      result(lists.strings) mustEqual List("One", "Two", "Three")
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val test = new Table("test") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", Unique)
    val date = column[Long]("date")
  }
  val suppliers = new Table("SUPPLIERS") {
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
  }
  val coffees = new Table("COFFEES") {
    val name = column[String]("COF_NAME", PrimaryKey)
    val supID = column[Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
  }
  val names = new Table("names") {
    val name = column[String]("name", PrimaryKey, Unique, NotNull)
    val age = column[Int]("age", NotNull)
  }

  val tables = List(test, suppliers, coffees, names)
}

object TestCrossReferenceDatastore extends H2Datastore(mode = H2Memory("cross_reference")) {
  val first = new Table("first") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val secondId = column[Int]("secondId")
  }
  val second = new Table("second") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val value = column[Int]("value")
    val firstId = column[Int]("firstId", new ForeignKey(first.id))
  }
  first.secondId.props(new ForeignKey(second.id))

  val tables = List(first, second)
}

object TestSpecialTypesDatastore extends H2Datastore(mode = H2Memory("special_types")) {
  implicit val listStringConverter = new ColumnConverter[List[String]] {
    def sqlType = StringConverter.sqlType

    def toSQLType(column: ColumnLike[List[String]], value: List[String]) = value.mkString("|")

    def fromSQLType(column: ColumnLike[List[String]], value: Any) = value match {
      case null => Nil
      case s: String => s.split('|').toList
    }
  }

  val lists = new Table("special_lists") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val strings = column[List[String]]("strings")
  }

  val tables = List(lists)
}