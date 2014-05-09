package com.outr.query.h2

import com.outr.query._
import com.outr.query.column.property._
import com.outr.query.convert.{ObjectSerializationConverter, StringConverter, ColumnConverter}
import java.sql.{Timestamp, Blob}
import javax.sql.rowset.serial.SerialBlob
import org.powerscala.IO

import com.outr.query.table.property.Index
import org.h2.jdbc.JdbcSQLException
import com.outr.query.h2.trigger.TriggerType
import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends WordSpec with Matchers {
  import TestDatastore._

  var acmeId: Int = _
  var superiorId: Int = _
  var highGroundId: Int = _

  "test" should {
    "have two columns" in {
      test.columns.size should equal(3)
    }
    "have the proper table-to-table relationships set" in {
      test.one2One.size should equal(0)
      test.one2Many.size should equal(0)
      test.many2One.size should equal(0)
      test.many2Many.size should equal(0)
    }
    "verify the create table String is correct" in {
      val sql = TestDatastore.createTableSQL(ifNotExist = true, test)
      sql should equal("CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT, name VARCHAR(2147483647) UNIQUE, date TIMESTAMP, PRIMARY KEY(id));")
    }
    "create the table" in {
      create()
    }
    "insert a record" in {
      val id = insert(test.name("John Doe")).toList.head
      id should equal(1)
    }
    "create a simple query" in {
      val q = select(test.id, test.name) from test
      q.expressions.size should equal(2)
    }
    "query the record back out" in {
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
      results.size should equal(1)
      val john = results.head
      john(test.id) should equal(1)
      john(test.name) should equal("John Doe")
    }
    "query a record back via 'LIKE'" in {
      val query = select(test.id, test.name) from test where test.name % "John%"
      val results = exec(query).toList
      results.size should equal(1)
      val john = results.head
      john(test.id) should equal(1)
      john(test.name) should equal("John Doe")
    }
    "insert another record" in {
      insert(test.name("Jane Doe"))
    }
    "query the record back by name" in {
      val query = select(test.id, test.name) from test where test.name === "Jane Doe"
      val results = exec(query).toList
      results.size should equal(1)
      val jane = results.head
      jane(test.id) should equal(2)
      jane(test.name) should equal("Jane Doe")
    }
    "query with multiple where clauses" in {
      val query = select (test.id, test.name) from test where (test.name === "Jane Doe" or test.name === "John Doe") and test.id > 0
      val results = exec(query).toList
      results.size should equal(2)
    }
    "query two records back via regular expression" in {
      val query = select(test.id, test.name) from test where test.name * ".*Doe".r
      val results = exec(query).toList
      results.size should equal(2)
      val john = results.head
      john(test.id) should equal(1)
      john(test.name) should equal("John Doe")
      val jane = results.tail.head
      jane(test.id) should equal(2)
      jane(test.name) should equal("Jane Doe")
    }
    "update 'John Doe' to 'Joe Doe'" in {
      val updated = exec(update(test.name("Joe Doe")) where(test.name === "John Doe"))
      updated should equal(1)
    }
    "verify that 'John Doe' no longer exists" in {
      val query = select(test.name) from test where test.name === "John Doe"
      val results = exec(query).toList
      results.size should equal(0)
    }
    "verify that 'Joe Doe' does exist" in {
      val query = select(test.name) from test where test.name === "Joe Doe"
      val results = exec(query).toList
      results.size should equal(1)
    }
    "verify that 'Jane Doe' wasn't modified" in {
      val query = select(test.name) from test where test.name === "Jane Doe"
      val results = exec(query).toList
      results.size should equal(1)
    }
    "delete 'Joe Doe' from the database" in {
      val deleted = exec(delete(test) where test.name === "Joe Doe")
      deleted should equal(1)
    }
    "verify there is just one record left in the database" in {
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
      results.size should equal(1)
      val jane = results.head
      jane(test.id) should equal(2)
      jane(test.name) should equal("Jane Doe")
    }
    "delete everything from the database" in {
      val deleted = exec(delete(test))
      deleted should equal(1)
    }
    "verify there are no records left in the database" in {
      val query = select(test.id, test.name) from test
      val results = exec(query).toList
      results.size should equal(0)
    }
  }
  "suppliers" should {
    import Suppliers._
    "have the proper table-to-table relationships set" in {
      suppliers.one2One.size should equal(0)
      suppliers.one2Many.size should equal(0)
      suppliers.many2One.size should equal(1)
      suppliers.many2Many.size should equal(0)
    }
    "insert three suppliers" in {
      acmeId = insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).get
      superiorId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).get
      highGroundId = insert(name("The High Ground"), street("100 Coffee Lane"), city("Meadows"), state("CA"), zip("93966")).get
      acmeId shouldNot equal(0)
      superiorId shouldNot equal(0)
      highGroundId shouldNot equal(0)
    }
  }
  "coffees" should {
    import Coffees._
    "have the proper table-to-table relationships set" in {
      coffees.one2One.size should equal(0)
      coffees.one2Many.size should equal(1)
      coffees.many2One.size should equal(0)
      coffees.many2Many.size should equal(0)
    }
    "insert five coffees" in {
      insert(name("Colombian"), supID(acmeId), price(7.99), sales(0), total(0))
      insert(name("French Roast"), supID(superiorId), price(8.99), sales(0), total(0))
      insert(name("Espresso"), supID(highGroundId), price(9.99), sales(0), total(0))
      insert(name("Colombian Decaf"), supID(acmeId), price(8.99), sales(0), total(0))
      insert(name("French Roast Decaf"), supID(superiorId), price(9.99), sales(0), total(0))
    }
    "query five coffees back out" in {
      val results = exec(select(*) from coffees).toList
      results.size should equal(5)
    }
    "query joining suppliers" in {
      val query = select(name, supID, price, sales, total, suppliers.name) from coffees innerJoin suppliers on suppliers.id === supID
      val results = exec(query).toList
      results.size should equal(5)
      val first = results.head
      first.values.size should equal(6)
      first(suppliers.name) should equal("Acme, Inc.")
    }
    "query the minimum price" in {
      val query = select(price.min) from coffees
      val results = exec(query).toList
      results.size should equal(1)
      val values = results.head
      values.values.size should equal(1)
      val minimumPrice = values(price.min)
      minimumPrice should equal(7.99)
    }
    "query the count of coffees for Superior Coffee" in {
      val query = select(name.count) from coffees innerJoin suppliers on supID === suppliers.id where suppliers.name === "Superior Coffee"
      val results = exec(query).toList
      results.size should equal(1)
      val values = results.head
      values.values.size should equal(1)
      val count = values(name.count)
      count should equal(2)
    }
    "query with an inner join aliased" in {
      val s = suppliers as "s"
      val query = select(name, s(suppliers.name)) from coffees innerJoin s on supID === s(suppliers.id)
      val results = exec(query).toList
      results.size should equal(5)
    }
    "query coffees ordered by name and limited to the second result" in {
      val query = select(name) from coffees orderBy(name asc) limit 1 offset 1
      val results = exec(query).toList
      results.size should equal(1)
      val values = results.head
      values.values.size should equal(1)
      val coffeeName = values(name)
      coffeeName should equal("Colombian Decaf")
    }
    "query coffees grouped by price" in {
      val query = select(price) from coffees groupBy price orderBy(price asc)
      val results = exec(query).toList
      results.size should equal(3)
      results(0)(price) should equal(7.99)
      results(1)(price) should equal(8.99)
      results(2)(price) should equal(9.99)
    }
  }
  "names" should {
    import Names._

    val queryAll = select(*) from names orderBy(name asc)

    "have no records in the table" in {
      val results = exec(queryAll).toList
      results.size should equal(0)
    }
    "merge 'John Doe' for an inserted record" in {
      merge(name, name("John Doe"), age(21))
      val results = exec(queryAll).toList
      results.size should equal(1)
      val result = results.head
      result(name) should equal("John Doe")
      result(age) should equal(21)
    }
    "merge 'John Doe' for an updated record" in {
      merge(name, name("John Doe"), age(25))
      val results = exec(queryAll).toList
      results.size should equal(1)
      val result = results.head
      result(name) should equal("John Doe")
      result(age) should equal(25)
    }
    "merge 'Jane Doe' for an inserted record" in {
      merge(name, name("Jane Doe"), age(22))
      val results = exec(queryAll).toList
      results.size should equal(2)
      val jane = results.head
      jane(name) should equal("Jane Doe")
      jane(age) should equal(22)
      val john = results.tail.head
      john(name) should equal("John Doe")
      john(age) should equal(25)
    }
  }
  "fruit colors" should {
    import FruitColors._

    "insert an Orange" in {
      insert(color("Orange"), fruit(Fruit("Orange")))
    }
    "query the Orange back" in {
      val results = exec(select (*) from fruitColors).toList
      results.size should equal(1)
      val orange = results.head
      orange(color) should equal("Orange")
      orange(fruit) should equal(Fruit("Orange"))
    }
  }
  "TestCrossReferenceDatastore" should {
    "create the tables successfully" in {
      TestCrossReferenceDatastore.create()
    }
  }
  "TestSpecialTypesDatastore" should {
    import SpecialTypesDatastore._

    var listId: Int = -1
    var dataId: Int = -1
    var inserted = 0
    var updated = 0
    var deleted = 0
    var selected = 0

    "create the tables successfully" in {
      create()
    }
    "insert a List[String] entry" in {
      val idOption = insert(lists.strings(List("One", "Two", "Three")))
      idOption shouldNot equal(None)
      listId = idOption.get
      listId should equal(1)
    }
    "query a List[String] entry" in {
      val query = select(lists.id, lists.strings) from lists
      val results = exec(query).toList
      results.size should equal(1)
      val result = results.head
      result(lists.id) should equal(listId)
      result(lists.strings) should equal(List("One", "Two", "Three"))
    }
    "insert a Blob entry" in {
      val idOption = insert(data.content(new SerialBlob("test using blob".getBytes("UTF-8"))))
      idOption shouldNot equal(None)
      dataId = idOption.get
      dataId should equal(1)
    }
    "query a Blob entry" in {
      val query = select(data.id, data.content) from data
      val results = exec(query).toList
      results.size should equal(1)
      val result = results.head
      result(data.id) should equal(dataId)
      val content = result(data.content)
      val s = IO.copy(content.getBinaryStream)
      s should equal("test using blob")
    }
    "insert John Doe into combinedUnique" in {
      insert(combinedUnique.firstName("John"), combinedUnique.lastName("Doe")) should equal(Some(1))
    }
    "insert Jane Doe into combinedUnique" in {
      insert(combinedUnique.firstName("Jane"), combinedUnique.lastName("Doe")) should equal(Some(2))
    }
    "attempting to insert John Doe again throws a constraint violation" in {
      val exc = intercept[JdbcSQLException] {
        insert(combinedUnique.firstName("John"), combinedUnique.lastName("Doe"))
        fail()
      }
    }
    "add a trigger" in {
      trigger.on {
        case evt => evt.triggerType match {
          case TriggerType.Insert => inserted += 1
          case TriggerType.Update => updated += 1
          case TriggerType.Delete => deleted += 1
          case TriggerType.Select => selected += 1
        }
      } shouldNot equal(null)
    }
    "validate no trigger has been invoked" in {
      inserted should equal(0)
      updated should equal(0)
      deleted should equal(0)
      selected should equal(0)
    }
    "insert a record to fire a trigger" in {
      insert(triggerTest.name("Test1")) should equal(Some(1))
    }
    "validate that one insert was triggered" in {
      inserted should equal(1)
      updated should equal(0)
      deleted should equal(0)
      selected should equal(0)
    }
    "update a record to fire a trigger" in {
      exec(update(triggerTest.name("Test2")) where triggerTest.id === 1) should equal(1)
    }
    "validate that one update was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(0)
      selected should equal(0)
    }
    "select a record to fire a select trigger" in {
      val results = exec(select(triggerTest.*) from triggerTest).toList
      results.size should equal(1)
    }
    "validate that another update was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(0)
      selected should equal(1)
    }
    "delete a record to fire a trigger" in {
      exec(delete(triggerTest) where triggerTest.id === 1) should equal(1)
    }
    "validate that one delete was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(1)
      selected should equal(1)
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
  val date = column[Timestamp]("date")
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

object SpecialTypesDatastore extends H2Datastore(mode = H2Memory("special_types")) {
  def lists = Lists
  def data = Data
  def combinedUnique = CombinedUnique
  def triggerTest = TriggerTest
}

object Lists extends Table(SpecialTypesDatastore) {
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

object Data extends Table(SpecialTypesDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val content = column[Blob]("content")
}

object CombinedUnique extends Table(SpecialTypesDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val firstName = column[String]("firstName", NotNull)
  val lastName = column[String]("lastName", NotNull)

  props(Index.unique("IDXNAME", firstName, lastName))
}

object TriggerTest extends Table(SpecialTypesDatastore, Triggers.All) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", NotNull)
}

case class Fruit(name: String)