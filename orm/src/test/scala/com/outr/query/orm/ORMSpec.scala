package com.outr.query.orm

import com.outr.query.h2.H2Datastore
import com.outr.query.Table
import com.outr.query.column.property._
import com.outr.query.h2.H2Memory
import com.outr.query.orm.convert._
import java.sql.{Timestamp, Blob}
import java.io.File
import org.powerscala.IO
import com.outr.query.column.FileBlob
import com.outr.query.table.property.Linking
import org.scalatest.{Matchers, WordSpec}
import com.outr.query.orm.existing.ExistingQuery

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMSpec extends WordSpec with Matchers {
  import TestDatastore._

  private var bill: Person = _

  "Person" should {
    "create the tables" in {
      create()
    }
    "insert 'John Doe' into the table" in {
      val john = Person("John Doe")
      val updated = person.insert(john)
      updated.id should equal(Some(1))
    }
    "insert 'Jane Doe' into the table" in {
      val jane = Person("Jane Doe")
      val updated = person.insert(jane)
      updated.id should equal(Some(2))
    }
    "query back all records" in {
      val results = person.query(select(person.*) from person).toList
      results.size should equal(2)
    }
    "query back 'John Doe' with only 'name'" in {
      val results = person.query(select(person.name) from person where person.name === "John Doe").toList
      results.size should equal(1)
      val john = results.head
      john.id should equal(None)
      john.name should equal("John Doe")
    }
    "query back 'Jane Doe' with all fields" in {
      val results = person.query(select(person.*) from person where person.name === "Jane Doe").toList
      results.size should equal(1)
      val jane = results.head
      jane.id should equal(Some(2))
      jane.name should equal("Jane Doe")
    }
    "update 'Jane Doe' to 'Janie Doe'" in {
      val jane = person.query(select(person.*) from person where person.name === "Jane Doe").toList.head
      person.update(jane.copy(name = "Janie Doe"))
      val janie = person.query(select(person.*) from person where person.name === "Janie Doe").toList.head
      janie.name should equal("Janie Doe")
    }
    "delete 'Janie Doe' from the database" in {
      val janie = person.query(select(person.*) from person where person.name === "Janie Doe").toList.head
      person.delete(janie)
    }
    "insert person into database" in {
      bill = person.insert(Person("Bill Gates"))
      bill.id.get shouldNot equal(0)
    }
    "insert company into database" in {
      val microsoft = company.insert(Company("Microsoft", Lazy(bill)))
      microsoft.id.get shouldNot equal(0)
      microsoft.owner().id.get should equal(bill.id.get)
    }
    "query back company and lazy load the owner" in {
      val companies = company.query(select(company.*) from company).toList
      companies.size should equal(1)
      val microsoft = companies.head
      microsoft.name should equal("Microsoft")
      microsoft.owner.loaded should equal(false)
      val bill = microsoft.owner()
      bill.name should equal("Bill Gates")
      microsoft.owner.loaded should equal(true)
    }
    "insert company into database with a new owner" in {
      val steve = Person("Steve Jobs")
      val apple = company.insert(Company("Apple", Lazy(steve)))
      apple.id.get shouldNot equal(0)
      apple.owner().id.get shouldNot equal(0)
    }
    "query new person back out of database" in {
      val people = person.query(select(person.*) from person where person.name === "Steve Jobs").toList
      people.size should equal(1)
      val steve = people.head
      steve.name should equal("Steve Jobs")
    }
    "access company via LazyList in person" in {
      val people = person.query(select(person.*) from person where person.name === "Steve Jobs").toList
      people.size should equal(1)
      val steve = people.head
      val companies = steve.companies()
      companies.size should equal(1)
      val apple = companies.head
      apple.name should equal("Apple")
    }
    "insert some corporate domains" in {
      val apple = company.query(company.q where company.name === "Apple").toList.head
      apple.name should equal("Apple")
      val appleDomain = domain.insert(CorporateDomain("apple.com", apple))
      appleDomain.id shouldNot equal(None)
    }
    "query back the corporate domain with the company" in {
      val appleDomain = domain.query(domain.q).toList.head
      appleDomain.url should equal("apple.com")
      appleDomain.id.get should be > 0
      val apple = appleDomain.company
      apple shouldNot equal(null)
      apple.name should equal("Apple")
      apple.id.get should be > 0
    }
    "query partial and only persist partial" in {
      val steve = person.query(select(person.id, person.date) from person where person.name === "Steve Jobs").toList.head
      val newDate = System.currentTimeMillis()
      person.persist(steve.copy(date = newDate))
      val updated = person.query(person.q where person.name === "Steve Jobs").toList.head
      updated.name should equal("Steve Jobs")
      updated.date should equal(newDate)
    }
  }
  "SimpleInstance" should {
    "insert a single record without a 'modified' value and get a 'modified' value back" in {
      val original = SimpleInstance("test")
      original.modified should equal(0L)
      val current = System.currentTimeMillis()
      val record = simple.persist(original)
      record.modified should be >= current
    }
    "query the record back out with a modified value" in {
      val record = simple.query(simple.q).toList.head
      record.modified shouldNot equal(0L)
    }
  }
  "Orders and Items" should {
    "insert a few items and create some orders" in {
      val elmo = item.insert(Item("Tickle Me Elmo"))
      val district9 = item.insert(Item("District 9 DVD"))
      val batarang = item.insert(Item("Batarang"))

      elmo.id shouldNot equal(None)
      district9.id shouldNot equal(None)
      batarang.id shouldNot equal(None)

      order.insert(Order(LazyList(elmo))) shouldNot equal(null)
      order.insert(Order(LazyList(district9, batarang))) shouldNot equal(null)
      order.insert(Order(LazyList(district9, batarang, elmo))) shouldNot equal(null)
    }
    "simple query items are correct" in {
      val results = exec(item.q).toList
      results.size should equal(3)
    }
    "simple query orders are correct" in {
      val results = exec(order.q).toList
      results.size should equal(3)
    }
    "simple query orderItem entries are correct" in {
      val results = exec(select(orderItem.*) from orderItem).toList
      results.size should equal(6)
    }
    "query back the orders and verify the correct data" in {
      val orders = order.query(order.q).toList
      orders.size should equal(3)
      val o1 = orders.head
      o1.items().length should equal(1)
      o1.items().head.name should equal("Tickle Me Elmo")
      val o2 = orders.tail.head
      o2.items().length should equal(2)
      o2.items().head.name should equal("District 9 DVD")
      o2.items().tail.head.name should equal("Batarang")
      val o3 = orders.tail.tail.head
      o3.items().length should equal(3)
      o3.items().head.name should equal("District 9 DVD")
      o3.items().tail.head.name should equal("Batarang")
      o3.items().tail.tail.head.name should equal("Tickle Me Elmo")
    }
    "remove all the orders" in {
      val orders = order.query().toList
      orders.foreach {
        case order => {
          exec(delete(OrderItem) where OrderItem.orderId === order.id.get)
          Order.delete(order)
        }
      }
    }
    "verify no orders are in the system" in {
      order.query().toList should equal(Nil)
    }
    "create one order with one item" in {
      val item = Item.query(Item.q where Item.name === "Tickle Me Elmo").head
      val order = Order(items = LazyList(item))
      Order.persist(order)
    }
    "verify one order exists with one item" in {
      val orders = Order.query().toList
      orders.size should equal(1)
      val order = orders.head
      order.items().size should equal(1)
      order.items().head.name should equal("Tickle Me Elmo")
    }
    "update the order with a different item" in {
      val item = Item.query(Item.q where Item.name === "District 9 DVD").head
      val order = Order.query().head
      val updated = order.copy(items = LazyList(item))
      Order.persist(updated)
    }
    "verify one order exists with the new item" in {
      val orders = Order.query().toList
      orders.size should equal(1)
      val order = orders.head
      order.items().size should equal(1)
      order.items().head.name should equal("District 9 DVD")
    }
  }
  "Countries" should {
    val PopulationIn2011 = 311600000
    val PopulationIn2012 = 313900000

    "insert one country" in {
      country.merge(Country("USA", PopulationIn2011))
    }
    "query the country back" in {
      val results = country.query(country.q).toList
      results.size should equal(1)
      val usa = results.head
      usa.name should equal("USA")
      usa.population should equal(PopulationIn2011)
    }
    "merge a population change" in {
      country.merge(Country("USA", PopulationIn2012))
    }
    "query only one country back" in {
      val results = country.query(country.q).toList
      results.size should equal(1)
      val usa = results.head
      usa.name should equal("USA")
      usa.population should equal(PopulationIn2012)
    }
  }
  "Content" should {
    val testContent = "Testing content"

    "insert content and data" in {
      val file = File.createTempFile("tmp", ".txt")
      IO.copy(testContent, file)
      val data = contentData.persist(ContentData(new FileBlob(file)))
      data.id shouldNot equal(None)
      val c = content.persist(Content("Test", Transient(data)))
      c.id shouldNot equal(None)
    }
    "query back content and access data" in {
      val results = content.query(content.q).toList
      results.size should equal(1)
      val c = results.head
      c.name should equal("Test")
      c.data.use {
        case Some(data) => {
          val s = new String(data.content.getBytes(0, data.content.length().toInt), "UTF-8")
          s should equal(testContent)
        }
        case None => throw new RuntimeException("No result found!")
      }
    }
    "remove data" in {
      val results = content.query(content.q).toList
      results.size should equal(1)
      val c = results.head
      c.name should equal("Test")
      val updated = content.persist(c.copy(name = "new name", data = Transient.None))
      updated.data should equal(Transient.None)
    }
    "set data back" in {
      val results = content.query(content.q).toList
      results.size should equal(1)
      val c = results.head
      c.name should equal("new name")
      c.data.use {
        case Some(data) => {
          val s = new String(data.content.getBytes(0, data.content.length().toInt), "UTF-8")
          s should equal(testContent)
        }
        case None => throw new RuntimeException("No result found!")
      }
    }
  }
  "User" should {
    "insert an Administrator" in {
      user.persist(Administrator("Super User")) shouldNot equal(null)
    }
    "query back the Administrator by name" in {
      val results = user.query(user.q where user.name === "Super User").toList
      results should have length 1
      val result = results.head
      result.getClass should equal(classOf[Administrator])
      result.name should equal("Super User")
    }
    "insert a Developer" in {
      user.persist(Developer("Superman", "Scala")) shouldNot equal(null)
    }
    "query back the Developer by name" in {
      val results = user.query(user.q where user.name === "Superman").toList
      results should have length 1
      val result = results.head
      result.getClass should equal(classOf[Developer])
      result.name should equal("Superman")
      result.asInstanceOf[Developer].language should equal("Scala")
    }
    "query back all users" in {
      val results = user.query(user.q).toList
      results should have length 2
      val admin = results.collectFirst {
        case u: Administrator => u
      }.get
      admin.name should equal("Super User")
      val dev = results.collectFirst {
        case u: Developer => u
      }.get
      dev.name should equal("Superman")
      dev.language should equal("Scala")
    }
  }
  "existing query" should {
    val queryString = "SELECT id, name, language FROM user WHERE id = ?"
    val existingQuery = new ExistingQuery[ExistingResult](TestDatastore, queryString)
    "query back a specific result" in {
      val results = existingQuery.query(List(2)).toList
      results.length should equal(1)
      val result = results.head
      result.id should equal(2)
      result.name should equal("Superman")
      result.language should equal("Scala")
    }
  }
  "validating corner cases" should {
    "create a company, set the owner, then verify it queries back with the user properly" in {
      val company = SpecialCompany.persist(SpecialCompany("Superco"))
      val company2 = SpecialCompany.persist(company.copy(name = "Supercompany"))
      val user = Person.persist(Person("Me"))
      SpecialCompany.persist(company.copy(owner = Transient(user)))
      val updated = SpecialCompany.byId(company.id.get).get
      updated.owner.use(o => o.get.name should equal("Me"))
    }
  }
  "persistence with exception handling" should {
    "insert a unique name successfully" in {
      UniqueName.persist(UniqueName("johndoe"))
    }
    "insert the same unique name and auto-correct" in {
      UniqueName.persist(UniqueName("johndoe"))
    }
    "query back two distinct UniqueNames" in {
      val names = UniqueName.query(UniqueName.q).toVector
      names.length should equal(2)
      names(0).name should equal("johndoe")
      names(1).name should equal("johndoe1")
    }
  }
  "persistence with object converter that must not be null" should {
    "insert a named user" in {
      NamedUser.persist(NamedUser("Test User", UniqueName("testing")))
    }
    "query back the named user" in {
      val namedOption = NamedUser.query(NamedUser.q where UniqueName.name === "testing").headOption
      namedOption shouldNot equal(None)
      val named = namedOption.get
      named.id shouldNot equal(None)
      named.username shouldNot equal(null)
      named.username.name should equal("testing")
    }
    "update the test user" in {
      val user = NamedUser.query().head
      user.name should equal("Test User")
      NamedUser.persist(user.copy(name = "Testing User"))
    }
    "verify the name changed properly" in {
      val user = NamedUser.query().head
      user.name should equal("Testing User")
      user.username.name should equal("testing")
    }
    "update the username" in {
      val user = NamedUser.query().head
      NamedUser.persist(user.copy(username = user.username.copy(name = "testuser")))
    }
    "verify the username changed properly" in {
      val user = NamedUser.query().head
      user.name should equal("Testing User")
      user.username.name should equal("testuser")
    }
    "query user by id" in {
      val user = NamedUser.byId(1).get
      user.name should equal("Testing User")
      user.username.name should equal("testuser")
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  def person = Person
  def company = Company
  def domain = CorporateDomain
  def simple = SimpleInstance
  def order = Order
  def item = Item
  def orderItem = OrderItem
  def country = Country
  def contentData = ContentData
  def content = Content
  def user = User
  def specialCompany = SpecialCompany
  def uniqueName = UniqueName
  def namedUser = NamedUser

  LazyList.connect[Person, Company, Int](person, "companies", company.ownerId)
  LazyList.connect[Order, Item, Int](order, "items", orderItem.itemId, item, "orders", orderItem.orderId)
}

case class Person(name: String, date: Long = System.currentTimeMillis(), companies: LazyList[Company] = LazyList.Empty, id: Option[Int] = None)

object Person extends ORMTable[Person](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
  val date = orm[Timestamp, Long]("date")
}

case class Company(name: String, owner: Lazy[Person] = Lazy.None, id: Option[Int] = None)

object Company extends ORMTable[Company](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
  val ownerId = orm[Int, Lazy[Person]]("ownerId", "owner", new LazyConverter[Person], new ForeignKey(Person.id))
}

case class SpecialCompany(name: String, owner: Transient[Person] = Transient.None, id: Option[Int] = None)

object SpecialCompany extends ORMTable[SpecialCompany](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
  val ownerId = orm[Int, Transient[Person]]("ownerId", "owner", new TransientConverter[Person], new ForeignKey(Person.id))
}

case class CorporateDomain(url: String, company: Company, id: Option[Int] = None)

object CorporateDomain extends ORMTable[CorporateDomain](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val url = orm[String]("url", Unique)
  val companyId = orm[Int, Company]("companyId", "company", new ObjectConverter[Company], new ForeignKey(Company.id))
}

case class SimpleInstance(name: String, modified: Long = 0L, id: Option[Int] = None)

object SimpleInstance extends ORMTable[SimpleInstance](TestDatastore) with ModifiedSupport[SimpleInstance] {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name")
  val modified = orm[Long]("modified", NotNull)
}

case class Order(items: LazyList[Item] = LazyList.Empty, date: Long = System.currentTimeMillis(), id: Option[Int] = None)

object Order extends ORMTable[Order](TestDatastore, "orders") {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val date = orm[Long]("date", NotNull)
}

case class Item(name: String, orders: LazyList[Order] = LazyList.Empty, id: Option[Int] = None)

object Item extends ORMTable[Item](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", NotNull)
}

object OrderItem extends Table(TestDatastore, Linking) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val orderId = column[Int]("orderId", new ForeignKey(Order.id))
  val itemId = column[Int]("itemId", new ForeignKey(Item.id))
}

case class Country(name: String, population: Int)

object Country extends ORMTable[Country](TestDatastore) {
  val name = orm[String]("name", Unique, NotNull, PrimaryKey)
  val population = orm[Int]("population", NotNull)
}

case class ContentData(content: Blob, id: Option[Int] = None)

object ContentData extends ORMTable[ContentData](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val content = orm[Blob]("content", NotNull)
}

case class Content(name: String, data: Transient[ContentData] = Transient.None, id: Option[Int] = None)

object Content extends ORMTable[Content](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", NotNull)
  val data = orm[Int, Transient[ContentData]]("dataId", "data", new TransientConverter[ContentData], new ForeignKey(ContentData.id))
}

trait User {
  def id: Option[Int]
  def name: String
}

object User extends PolymorphicORMTable[User](TestDatastore) {
  val Administrator = 1
  val Developer = 2
  val Employee = 3

  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", NotNull)
  val language = orm[String]("language", MappingOptional)
  val userType = column[Int]("userType", NotNull)

  override val caseClasses = Vector(classOf[Administrator], classOf[Developer], classOf[Employee])
  def typeColumn = userType
}

case class Administrator(name: String, id: Option[Int] = None) extends User

case class Developer(name: String, language: String, id: Option[Int] = None) extends User

case class Employee(name: String, id: Option[Int] = None) extends User

case class ExistingResult(id: Int, name: String, language: String)

case class UniqueName(name: String, id: Option[Int] = None) {
  def incrementName = name match {
    case UniqueName.IncrementedName(value, number) => copy(name = s"$value${number.toInt + 1}")
    case _ => copy(name = s"${name}1")
  }
}

object UniqueName extends ORMTable[UniqueName](TestDatastore) {
  private val IncrementedName = """(.*)(\d+)""".r

  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", NotNull, Unique)

  persistFailing.on {
    case (un, t) if t.getMessage.contains("PUBLIC.UNIQUE_NAME(NAME)") => Some(un.incrementName)
    case _ => None
  }
}

case class NamedUser(name: String, username: UniqueName, id: Option[Int] = None) {
  if (username == null) throw new NullPointerException("Username must not be null!")
}

object NamedUser extends ORMTable[NamedUser](TestDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", NotNull)
  val usernameId = orm[Int, UniqueName]("usernameId", "username", new ObjectConverter[UniqueName], new ForeignKey(UniqueName.id), NotNull, Unique)
}