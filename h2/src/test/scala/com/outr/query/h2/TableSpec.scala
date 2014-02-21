package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._
import com.outr.query.property._
import com.outr.query.convert.{ObjectSerializationConverter, StringConverter, ColumnConverter}
import org.specs2.main.ArgumentsShortcuts
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob
import org.powerscala.IO

import scala.language.reflectiveCalls
import com.outr.query.table.property.Index
import org.h2.jdbc.JdbcSQLException
import com.outr.query.column.property.Searchable
import com.outr.query.h2.trigger.TriggerType

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
      sql mustEqual "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT, name VARCHAR(2147483647) UNIQUE, date BIGINT, PRIMARY KEY(id));"
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
    "query a record back via 'LIKE'" in {
      val query = select(test.id, test.name) from test where test.name % "John%"
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
    "query two records back via regular expression" in {
      val query = select(test.id, test.name) from test where test.name * ".*Doe".r
      val results = exec(query).toList
      results must have size 2
      val john = results.head
      john(test.id) mustEqual 1
      john(test.name) mustEqual "John Doe"
      val jane = results.tail.head
      jane(test.id) mustEqual 2
      jane(test.name) mustEqual "Jane Doe"
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
    import Suppliers._
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
    import Coffees._
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
    import Names._

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
  "fruit colors" should {
    import FruitColors._

    "insert an Orange" in {
      insert(color("Orange"), fruit(Fruit("Orange"))) must not(throwA[Throwable])
    }
    "query the Orange back" in {
      val results = exec(select (*) from fruitColors).toList
      results must have size 1
      val orange = results.head
      orange(color) mustEqual "Orange"
      orange(fruit) mustEqual Fruit("Orange")
    }
  }
  "TestCrossReferenceDatastore" should {
    "create the tables successfully" in {
      TestCrossReferenceDatastore.create() must not(throwA[Throwable])
    }
  }
  "TestSpecialTypesDatastore" should {
    import TestSpecialTypesDatastore._

    var listId: Int = -1
    var dataId: Int = -1
    var inserted = 0
    var updated = 0
    var deleted = 0
    var selected = 0

    "create the tables successfully" in {
      create() must not(throwA[Throwable])
    }
    "insert a List[String] entry" in {
      val idOption = insert(lists.strings(List("One", "Two", "Three")))
      idOption mustNotEqual None
      listId = idOption.get
      listId mustEqual 1
    }
    "query a List[String] entry" in {
      val query = select(lists.id, lists.strings) from lists
      val results = exec(query).toList
      results must have size 1
      val result = results.head
      result(lists.id) mustEqual listId
      result(lists.strings) mustEqual List("One", "Two", "Three")
    }
    "insert a Blob entry" in {
      val idOption = insert(data.content(new SerialBlob("test using blob".getBytes("UTF-8"))))
      idOption mustNotEqual None
      dataId = idOption.get
      dataId mustEqual 1
    }
    "query a Blob entry" in {
      val query = select(data.id, data.content) from data
      val results = exec(query).toList
      results must have size 1
      val result = results.head
      result(data.id) mustEqual dataId
      val content = result(data.content)
      val s = IO.copy(content.getBinaryStream)
      s mustEqual "test using blob"
    }
    "insert John Doe into combinedUnique" in {
      insert(combinedUnique.firstName("John"), combinedUnique.lastName("Doe")) mustEqual Some(1)
    }
    "insert Jane Doe into combinedUnique" in {
      insert(combinedUnique.firstName("Jane"), combinedUnique.lastName("Doe")) mustEqual Some(2)
    }
    "attempting to insert John Doe again throws a constraint violation" in {
      insert(combinedUnique.firstName("John"), combinedUnique.lastName("Doe")) must throwA[JdbcSQLException]
    }
    "add a trigger" in {
      trigger.on {
        case evt => evt.triggerType match {
          case TriggerType.Insert => inserted += 1
          case TriggerType.Update => updated += 1
          case TriggerType.Delete => deleted += 1
          case TriggerType.Select => selected += 1
        }
      } mustNotEqual null
    }
    "validate no trigger has been invoked" in {
      inserted mustEqual 0
      updated mustEqual 0
      deleted mustEqual 0
      selected mustEqual 0
    }
    "insert a record to fire a trigger" in {
      insert(triggerTest.name("Test1")) mustEqual Some(1)
    }
    "validate that one insert was triggered" in {
      inserted mustEqual 1
      updated mustEqual 0
      deleted mustEqual 0
      selected mustEqual 0
    }
    "update a record to fire a trigger" in {
      exec(update(triggerTest.name("Test2")) where triggerTest.id === 1) mustEqual 1
    }
    "validate that one update was triggered" in {
      inserted mustEqual 1
      updated mustEqual 1
      deleted mustEqual 0
      selected mustEqual 0
    }
    "select a record to fire a select trigger" in {
      val results = exec(select(triggerTest.*) from triggerTest).toList
      results must have size 1
    }
    "validate that one update was triggered" in {
      inserted mustEqual 1
      updated mustEqual 1
      deleted mustEqual 0
      selected mustEqual 1
    }
    "delete a record to fire a trigger" in {
      exec(delete(triggerTest) where triggerTest.id === 1) mustEqual 1
    }
    "validate that one delete was triggered" in {
      inserted mustEqual 1
      updated mustEqual 1
      deleted mustEqual 1
      selected mustEqual 1
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  def test = TestTable
  def suppliers = Suppliers
  def coffees = Coffees
  def names = Names
  def fruitColors = FruitColors
}

object TestTable extends Table(TestDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", Unique)
  val date = column[Long]("date")
}

object Suppliers extends Table(TestDatastore) {
  val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
  val name = column[String]("SUP_NAME")
  val street = column[String]("STREET")
  val city = column[String]("CITY")
  val state = column[String]("STATE")
  val zip = column[String]("ZIP")
}

object Coffees extends Table(TestDatastore) {
  val name = column[String]("COF_NAME", PrimaryKey)
  val supID = column[Int]("SUP_ID", new ForeignKey(TestDatastore.suppliers.id))
  val price = column[Double]("PRICE")
  val sales = column[Int]("SALES")
  val total = column[Int]("TOTAL")
}

object Names extends Table(TestDatastore) {
  val name = column[String]("name", PrimaryKey, Unique, NotNull)
  val age = column[Int]("age", NotNull, Indexed("idxage"))
}

object FruitColors extends Table(TestDatastore) {
  val color = column[String]("color", NotNull)
  val fruit = column[Fruit]("fruit", new ObjectSerializationConverter[Fruit], NotNull)
}

object TestCrossReferenceDatastore extends H2Datastore(mode = H2Memory("cross_reference")) {
  First.secondId.props(new ForeignKey(Second.id))
}

object First extends Table(TestCrossReferenceDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name")
  val secondId = column[Int]("secondId")
}

object Second extends Table(TestCrossReferenceDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val value = column[Int]("value")
  val firstId = column[Int]("firstId", new ForeignKey(First.id))
}

object TestSpecialTypesDatastore extends H2Datastore(mode = H2Memory("special_types")) {
  def lists = Lists
  def data = Data
  def combinedUnique = CombinedUnique
  def triggerTest = TriggerTest
}

object Lists extends Table(TestSpecialTypesDatastore) {
  implicit val listStringConverter = new ColumnConverter[List[String]] {
    def sqlType(column: ColumnLike[List[String]]) = StringConverter.VarcharType

    def toSQLType(column: ColumnLike[List[String]], value: List[String]) = value.mkString("|")

    def fromSQLType(column: ColumnLike[List[String]], value: Any) = value match {
      case null => Nil
      case s: String => s.split('|').toList
    }
  }

  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val strings = column[List[String]]("strings")
}

object Data extends Table(TestSpecialTypesDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val content = column[Blob]("content")
}

object CombinedUnique extends Table(TestSpecialTypesDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val firstName = column[String]("firstName", NotNull)
  val lastName = column[String]("lastName", NotNull)

  props(Index.unique("IDXNAME", firstName, lastName))
}

object TriggerTest extends Table(TestSpecialTypesDatastore, Triggers.All) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", NotNull, Searchable)
}

case class Fruit(name: String)