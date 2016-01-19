package org.scalarelational

import java.io.{BufferedReader, InputStreamReader}
import java.sql.{Blob, Timestamp, Types}
import java.util.stream.Collectors
import javax.sql.rowset.serial.SerialBlob

import org.scalarelational.column.ColumnLike
import org.scalarelational.column.property._
import org.scalarelational.datatype._
import org.scalarelational.datatype.create.OptionDataTypeCreator
import org.scalarelational.instruction.query.TwoExpressions
import org.scalarelational.model._
import org.scalarelational.table.Table
import org.scalarelational.table.property.Index
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait AbstractTableSpec extends WordSpec with Matchers {
  val currentTime = System.currentTimeMillis()

  var acmeId: Int = _
  var superiorId: Int = _
  var highGroundId: Int = _
  
  def testDatastore: AbstractTestDatastore
  def testCrossReference: AbstractTestCrossReferenceDatastore
  def specialTypes: AbstractSpecialTypesDatastore

  protected def expectedNotNone: (String, Seq[TypedValue[_, _]]) = ("SELECT test_table.id FROM test_table WHERE test_table.id IS NOT ?", Seq(OptionDataTypeCreator.create[Int, Int](DataTypes.IntType).typed(null)))

  "test" should {
    val ds = testDatastore
    import ds._

    "have two columns" in {
      test.columns.size should equal(3)
    }
    "verify that there are no tables currently created" in {
      withSession { implicit session =>
        testDatastore.jdbcTables should equal(Set.empty)
        testDatastore.empty should equal(true)
      }
    }
    "create the table" in {
      withSession { implicit session =>
        create(test, suppliers, coffees, names, fruitColors)
      }
    }
    "verify that tables exist" in {
      withSession { implicit session =>
        testDatastore.jdbcTables shouldNot equal(Set.empty)
        testDatastore.empty should equal(false)
      }
    }
    "insert a record" in {
      withSession { implicit session =>
        val result = insert(test.name("John Doe")).result
        result should equal (1)
      }
    }
    "create a simple query" in {
      withSession { implicit session =>
        val q = select(test.id, test.name) from test
        q.expressions should equal (TwoExpressions(test.id, test.name))
      }
    }
    "query the record back out" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test
        val results = query.result.toList
        results.size should equal (1)
        val john = results.head
        john(test.id) should equal (Some(1))
        john(test.name) should equal ("John Doe")
      }
    }
    "query the record back out as a Tuple2" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test
        val results = query.converted
        results.next() should equal ((Some(1), "John Doe"))
      }
    }
    "query a record back via 'LIKE'" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test where test.name % "John%"
        val results = query.result.toList
        results.size should equal (1)
        val john = results.head
        john(test.id) should equal (Some(1))
        john(test.name) should equal ("John Doe")
      }
    }
    "insert another record" in {
      withSession { implicit session =>
        insert(test.name("Jane Doe")).result
      }
    }
    "query the record back by name" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test where test.name === "Jane Doe"
        val results = query.result.toList
        results.size should equal(1)
        val jane = results.head
        jane(test.id) should equal (Some(2))
        jane(test.name) should equal ("Jane Doe")
      }
    }
    "query with multiple where clauses" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test where (test.name === "Jane Doe" or test.name === "John Doe") and test.id > Some(0)
        val results = query.result.toList
        results.size should equal (2)
      }
    }
    "query with valid None comparison" in {
      withSession { implicit session =>
        val query = select (test.id) from test where test.id.!==(None) // !== conflicts with ScalaTest
        describe(query) should equal (
          expectedNotNone
        )
        val results = query.result.toList
        results.size should equal (2)
      }
    }
    "query with invalid None comparison" in {
      withSession { implicit session =>
        intercept[RuntimeException] {
          val query = select (test.id) from test where test.id > None
          query.result
        }
      }
    }
    "query two records back via regular expression" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test where test.name * ".*Doe".r orderBy(test.id asc)
        val results = query.result.toList
        results.size should equal (2)
        val john = results.head
        john(test.id) should equal (Some(1))
        john(test.name) should equal ("John Doe")
        val jane = results.tail.head
        jane(test.id) should equal (Some(2))
        jane(test.name) should equal ("Jane Doe")
      }
    }
    "update 'John Doe' to 'Joe Doe'" in {
      withSession { implicit session =>
        val updated = exec(update(test.name("Joe Doe")) where test.name === "John Doe")
        updated should equal(1)
      }
    }
    "verify that 'John Doe' no longer exists" in {
      withSession { implicit session =>
        val query = select(test.name) from test where test.name === "John Doe"
        val results = query.result.toList
        results.size should equal(0)
      }
    }
    "verify that 'Joe Doe' does exist" in {
      withSession { implicit session =>
        val query = select(test.name) from test where test.name === "Joe Doe"
        val results = query.result.toList
        results.size should equal(1)
      }
    }
    "verify that 'Jane Doe' wasn't modified" in {
      withSession { implicit session =>
        val query = select(test.name) from test where test.name === "Jane Doe"
        val results = query.result.toList
        results.size should equal(1)
      }
    }
    "delete 'Joe Doe' from the database" in {
      withSession { implicit session =>
        val deleted = exec(delete(test) where test.name === "Joe Doe")
        deleted should equal(1)
      }
    }
    "verify there is just one record left in the database" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test
        val results = query.result.toList
        results.size should equal(1)
        val jane = results.head
        jane(test.id) should equal (Some(2))
        jane(test.name) should equal("Jane Doe")
      }
    }
    "delete everything from the database" in {
      withSession { implicit session =>
        val deleted = exec(delete(test))
        deleted should equal(1)
      }
    }
    "verify there are no records left in the database" in {
      withSession { implicit session =>
        val query = select(test.id, test.name) from test
        val results = query.result.toList
        results.size should equal(0)
      }
    }
    "insert several new records asynchronously" in {
      import test._

      withSession { implicit session =>
        val future = insert(name("Adam")).
          and(name("Ben")).
          and(name("Chris")).
          and(name("Doug")).
          and(name("Evan")).
          and(name("Frank")).
          and(name("Greg")).
          and(name("Hank")).
          and(name("Ivan")).
          and(name("James")).
          and(name("Kevin")).
          and(name("Liam")).
          and(name("Matt")).
          and(name("Nathan")).
          and(name("Orville")).
          and(name("Phillip")).
          and(name("Quincy")).
          and(name("Rand")).
          and(name("Samuel")).
          and(name("Tom")).
          and(name("Uri")).
          and(name("Vladamir")).
          and(name("Walter")).
          and(name("Xavier")).
          and(name("Yasser")).
          and(name("Zach")).async
        val expected = if (supportsBatchInsertResponse) {
          List(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28)
        } else {
          List(28)    // Unfortunately a feature-limitation of some databases (H2 for example) is batch inserts only returns the last id
        }
        Await.result(future, 10.seconds) should equal(expected)
        (select(*) from test).result.toList.length should equal(26)
      }
    }
  }
  "suppliers" should {
    val ds = testDatastore
    import ds._
    import suppliers._
    "insert three suppliers" in {
      withSession { implicit session =>
        acmeId = insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).result
        superiorId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).result
        highGroundId = insert(name("The High Ground"), street("100 Coffee Lane"), city("Meadows"), state("CA"), zip("93966")).result
        acmeId shouldNot equal(0)
        superiorId shouldNot equal(0)
        highGroundId shouldNot equal(0)
      }
    }
  }
  "coffees" should {
    val ds = testDatastore
    import ds._
    import coffees._
    "insert five coffees" in {
      withSession { implicit session =>
        insert(name("Colombian"), supID(Some(acmeId)), price(7.99), sales(0), total(0)).
          and(name("French Roast"), supID(Some(superiorId)), price(8.99), sales(0), total(0)).
          and(name("Espresso"), supID(Some(highGroundId)), price(9.99), sales(0), total(0)).
          and(name("Colombian Decaf"), supID(Some(acmeId)), price(8.99), sales(0), total(0)).
          and(name("French Roast Decaf"), supID(Some(superiorId)), price(9.99), sales(0), total(0)).result
      }
    }
    "query five coffees back out" in {
      withSession { implicit session =>
        val results = (select(*) from coffees).result.toList
        results.size should equal(5)
      }
    }
    "query joining suppliers" in {
      withSession { implicit session =>
        val query = select(name, supID, price, sales, total, suppliers.name) from coffees innerJoin suppliers on suppliers.id === supID
        val results = query.result.toList
        results.size should equal(5)
        val first = results.head
        first.values.size should equal(6)
        first(suppliers.name) should equal("Acme, Inc.")
      }
    }
    "query the minimum price" in {
      withSession { implicit session =>
        val query = select(Min(price)) from coffees
        val results = query.result.toList
        results.size should equal(1)
        val values = results.head
        values.values.size should equal(1)
        val minimumPrice = values(Min(price))
        minimumPrice should equal(7.99)
      }
    }
    "query the count of coffees for Superior Coffee" in {
      withSession { implicit session =>
        val query = select(Count(name)) from coffees innerJoin suppliers on supID === suppliers.id where suppliers.name === "Superior Coffee"
        val results = query.result.toList
        results.size should equal(1)
        val values = results.head
        values.values.size should equal(1)
        val count = values(Count(name))
        count should equal(2)
      }
    }
    "query with an inner join aliased" in {
      withSession { implicit session =>
        val s = suppliers as "s"
        val query = select(name, s(suppliers.name)) from coffees innerJoin s on supID === s(suppliers.id)
        val results = query.result.toList
        results.size should equal(5)
      }
    }
    "query coffees ordered by name and limited to the second result" in {
      withSession { implicit session =>
        val query = select(name) from coffees orderBy (name asc) limit 1 offset 1
        val results = query.result.toList
        results.size should equal(1)
        val values = results.head
        values.values.size should equal(1)
        val coffeeName = values(name)
        coffeeName should equal("Colombian Decaf")
      }
    }
    "query coffees grouped by price" in {
      withSession { implicit session =>
        val query = select(price) from coffees groupBy price orderBy (price asc)
        val results = query.result.toVector
        results.size should equal(3)
        results.head(price) should equal(7.99)
        results(1)(price) should equal(8.99)
        results(2)(price) should equal(9.99)
      }
    }
    "insert coffee without a supplier" in {
      withSession { implicit session =>
        insert(name("Brown Stuff"), price(2.99), sales(0), total(0)).result
      }
    }
    "query coffees with suppliers via left join" in {
      withSession { implicit session =>
        val query = (
          select(name, price, suppliers.name.opt)
          from coffees
          leftJoin suppliers
          on coffees.supID === suppliers.id
          orderBy coffees.name.asc
        )
        val results = query.converted.toVector
        results.size should equal(6)

        def checkResult(index: Int, expectedName: String, expectedPrice: Double, expectedSupplierName: Option[String]): Unit = {
          val (n, p, s) = results(index)
          n should be(expectedName)
          p should be(expectedPrice)
          s should be(expectedSupplierName)
        }

        checkResult(0, "Brown Stuff", 2.99, None)
        checkResult(1, "Colombian", 7.99, Some("Acme, Inc."))
        checkResult(2, "Colombian Decaf", 8.99, Some("Acme, Inc."))
        checkResult(3, "Espresso", 9.99, Some("The High Ground"))
        checkResult(4, "French Roast", 8.99, Some("Superior Coffee"))
        checkResult(5, "French Roast Decaf", 9.99, Some("Superior Coffee"))
      }
    }
  }
  "names" should {
    val ds = testDatastore
    import ds._
    import names._

    val queryAll = select(*) from names orderBy(name asc)

    "have no records in the table" in {
      withSession { implicit session =>
        val results = queryAll.result.toList
        results.size should equal(0)
      }
    }
    if (supportsMerge) {
      "merge 'John Doe' for an inserted record" in {
        withSession { implicit session =>
          merge(name, name("John Doe"), age(21)).result
          val results = queryAll.result.toList
          results.size should equal(1)
          val result = results.head
          result(name) should equal("John Doe")
          result(age) should equal(21)
        }
      }
      "merge 'John Doe' for an updated record" in {
        withSession { implicit session =>
          merge(name, name("John Doe"), age(25)).result
          val results = queryAll.result.toList
          results.size should equal(1)
          val result = results.head
          result(name) should equal("John Doe")
          result(age) should equal(25)
        }
      }
      "merge 'Jane Doe' for an inserted record" in {
        withSession { implicit session =>
          merge(name, name("Jane Doe"), age(22)).result
          val results = queryAll.result.toList
          results.size should equal(2)
          val jane = results.head
          jane(name) should equal("Jane Doe")
          jane(age) should equal(22)
          val john = results.tail.head
          john(name) should equal("John Doe")
          john(age) should equal(25)
        }
      }
    }
    // TODO: fix support for this in PostgreSQL and then uncomment
    /*"insert and verify defaults" in {
      session {
        insert(names.name("Baby Doe"), names.age(1)).result
        val (n, a, c) = (select(names.name, names.age, names.created) from names where names.name === "Baby Doe").result.converted.one
        n should equal("Baby Doe")
        a should equal(1)
        c should be >= currentTime
      }
    }*/
  }
  "fruit colors" should {
    val ds = testDatastore
    import ds._
    import fruitColors._

    "insert an Orange" in {
      transaction { implicit session =>
        insert(color("Orange"), fruit(Fruit("Orange"))).result
      }
    }
    "query the Orange back" in {
      withSession { implicit session =>
        val results = (select(*) from fruitColors).result.toList
        results.size should equal(1)
        val orange = results.head
        orange(color) should equal("Orange")
        orange(fruit) should equal(Fruit("Orange"))
      }
    }
  }
  "TestCrossReferenceDatastore" should {
    val ds = testCrossReference
    import ds._
    "create the tables successfully" in {
      withSession { implicit session =>
        create(first, second)
      }
    }
  }
  "TestSpecialTypesDatastore" should {
    val ds = specialTypes
    import ds._

    var listId = -1
    var dataId = -1

    "create the tables successfully" in {
      withSession { implicit session =>
        create(lists, data, combinedUnique)
      }
    }
    "insert a List[String] entry" in {
      withSession { implicit session =>
        val idOption = insert(lists.strings(List("One", "Two", "Three")))
        idOption shouldNot equal(None)
        listId = idOption.result
        listId should equal (1)
      }
    }
    "query a List[String] entry" in {
      withSession { implicit session =>
        val query = select(lists.id, lists.strings) from lists
        val results = query.result.toList
        results.size should equal(1)
        val result = results.head
        result(lists.id) should equal (Some(listId))
        result(lists.strings) should equal(List("One", "Two", "Three"))
      }
    }
    "insert a Blob entry" in {
      transaction { implicit session =>
        dataId = insert(data.content(new SerialBlob("test using blob".getBytes("UTF-8")))).result
        dataId should equal (1)
      }
    }
    "query a Blob entry" in {
      withSession { implicit session =>
        val query = select(data.id, data.content) from data
        val results = query.result.toList
        results.size should equal(1)
        val result = results.head
        result(data.id) should equal (Some(dataId))
        val content = result(data.content)
        val reader = new BufferedReader(new InputStreamReader(content.getBinaryStream))
        try {
          val s = reader.lines().collect(Collectors.joining("\n"))
          s should equal("test using blob")
        } finally {
          reader.close()
        }
      }
    }
    "insert John Doe into combinedUnique" in {
      withSession { implicit session =>
        insert(
          combinedUnique.firstName("John"),
          combinedUnique.lastName("Doe")).result should equal (1)
      }
    }
    "insert Jane Doe into combinedUnique" in {
      withSession { implicit session =>
        insert(
          combinedUnique.firstName("Jane"),
          combinedUnique.lastName("Doe")).result should equal (2)
      }
    }
    "attempting to insert John Doe again throws a constraint violation" in {
      withSession { implicit session =>
        intercept[Throwable] {
          insert(combinedUnique.firstName("John"), combinedUnique.lastName("Doe")).result
          fail()
        }
      }
    }
  }
  "Cleanup" when {
    "using test" should {
      val ds = testDatastore
      import ds._

      "drop all tables" in {
        withSession { implicit session =>
          dropTable(test, cascade = true).result
          dropTable(coffees, cascade = true).result
          dropTable(suppliers, cascade = true).result
          dropTable(names, cascade = true).result
          dropTable(fruitColors, cascade = true).result
        }
      }
      "verify no tables exist anymore" in {
        withSession { implicit session =>
          jdbcTables should equal(Set.empty[String])
        }
      }
    }
    "using cross reference" should {
      val ds = testCrossReference
      import ds._

      "drop all tables" in {
        withSession { implicit session =>
          dropTable(first, cascade = true).result
          dropTable(second, cascade = true).result
        }
      }
      "verify no tables exist anymore" in {
        withSession { implicit session =>
          jdbcTables should equal(Set.empty[String])
        }
      }
    }
    "using special types" should {
      val ds = specialTypes
      import ds._

      "drop all tables" in {
        withSession { implicit session =>
          dropTable(lists, cascade = true).result
          dropTable(data, cascade = true).result
          dropTable(combinedUnique, cascade = true).result
        }
      }
      "verify no tables exist anymore" in {
        withSession { implicit session =>
          jdbcTables should equal(Set.empty[String])
        }
      }
    }
  }
}

trait AbstractTestDatastore extends Datastore {
  object test extends Table("test_table") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", Unique, ColumnLength(200))
    val date = column[Option[Timestamp], Timestamp]("date")
  }
  object suppliers extends Table("SUPPLIER") {
    val id = column[Option[Int], Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
  }
  object coffees extends Table("COFFEE") {
    val id = column[Option[Int], Int]("COF_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("COF_NAME", PrimaryKey)
    val supID = column[Option[Int], Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
  }
  object names extends Table("names") {
    val name = column[String]("name", Unique)
    val age = column[Int]("age", Indexed("idxage"))
    val created = column[Long, Timestamp]("created", Default("NOW()"))
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
  }
  object fruitColors extends Table("fruit_colors") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val color = column[String]("color")
    val fruit = column[Fruit, Blob]("fruit", ObjectSerializationDataTypeCreator.create[Fruit])
  }
}

trait AbstractTestCrossReferenceDatastore extends Datastore {
  object first extends Table("first") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val secondId = column[Int]("secondId", new ForeignKey(second.id))
  }

  object second extends Table("second") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val value = column[Int]("value")
    val firstId = column[Int]("firstId", new ForeignKey(first.id))
  }
}

trait AbstractSpecialTypesDatastore extends Datastore {
  object lists extends Table("lists") {
    object ListConverter extends SQLConversion[List[String], String] {
      override def toSQL(column: ColumnLike[List[String], String], value: List[String]): String = value.mkString("|")
      override def fromSQL(column: ColumnLike[List[String], String], value: String): List[String] = value.split('|').toList
    }
    implicit def listDataType = new DataType[List[String], String](Types.VARCHAR, SQLType("VARCHAR(1024)"), ListConverter)

    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val strings = column[List[String], String]("strings")
  }

  object data extends Table("data") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val content = column[Blob]("content")
  }

  object combinedUnique extends Table("combined_unique") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val firstName = column[String]("firstName")
    val lastName = column[String]("lastName")

    props(Index.unique("IDXNAME", firstName, lastName))
  }
}

case class Fruit(name: String)
