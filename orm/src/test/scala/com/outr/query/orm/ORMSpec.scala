package com.outr.query.orm

import org.specs2.mutable._
import com.outr.query.h2.H2Datastore
import com.outr.query.Column
import com.outr.query.property._
import scala.Some
import com.outr.query.h2.H2Memory

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMSpec extends Specification {
  import TestDatastore._

  private var bill: Person = _

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
      val results = person.query(select(person.*) from person).toList
      results must have size 2
    }
    "query back 'John Doe' with only 'name'" in {
      val results = person.query(select(person.name) from person where person.name === "John Doe").toList
      results must have size 1
      val john = results.head
      john.id mustEqual None
      john.name mustEqual "John Doe"
    }
    "query back 'Jane Doe' with all fields" in {
      val results = person.query(select(person.*) from person where person.name === "Jane Doe").toList
      results must have size 1
      val jane = results.head
      jane.id mustEqual Some(2)
      jane.name mustEqual "Jane Doe"
    }
    "update 'Jane Doe' to 'Janie Doe'" in {
      val jane = person.query(select(person.*) from person where person.name === "Jane Doe").toList.head
      person.update(jane.copy(name = "Janie Doe"))
      val janie = person.query(select(person.*) from person where person.name === "Janie Doe").toList.head
      janie.name mustEqual "Janie Doe"
    }
    "delete 'Janie Doe' from the database" in {
      val janie = person.query(select(person.*) from person where person.name === "Janie Doe").toList.head
      person.delete(janie) must not(throwA[Throwable])
    }
    "insert person into database" in {
      bill = person.insert(Person("Bill Gates"))
      bill.id.get mustNotEqual 0
    }
    "insert company into database" in {
      val microsoft = company.insert(Company("Microsoft", Lazy(bill)))
      microsoft.id.get mustNotEqual 0
      microsoft.owner().id.get mustEqual bill.id.get
    }
    "query back company and lazy load the owner" in {
      val companies = company.query(select(company.*) from company).toList
      companies must have size 1
      val microsoft = companies.head
      microsoft.name mustEqual "Microsoft"
      microsoft.owner.loaded mustEqual false
      val bill = microsoft.owner()
      bill.name mustEqual "Bill Gates"
      microsoft.owner.loaded mustEqual true
    }
    "insert company into database with a new owner" in {
      val steve = Person("Steve Jobs")
      val apple = company.insert(Company("Apple", Lazy(steve)))
      apple.id.get mustNotEqual 0
      apple.owner().id.get mustNotEqual 0
    }
    "query new person back out of database" in {
      val people = person.query(select(person.*) from person where person.name === "Steve Jobs").toList
      people must have size 1
      val steve = people.head
      steve.name mustEqual "Steve Jobs"
    }
    "access company via LazyList in person" in {
      val people = person.query(select(person.*) from person where person.name === "Steve Jobs").toList
      people must have size 1
      val steve = people.head
      val companies = steve.companies()
      companies must have size 1
      val apple = companies.head
      apple.name mustEqual "Apple"
    }
    "insert some corporate domains" in {
      val apple = company.query(company.q where company.name === "Apple").toList.head
      apple.name mustEqual "Apple"
      val appleDomain = domain.insert(CorporateDomain("apple.com", apple))
      appleDomain.id mustNotEqual None
    }
    "query back the corporate domain with the company" in {
      val appleDomain = domain.query(domain.q).toList.head
      appleDomain.url mustEqual "apple.com"
      appleDomain.id.get must be > 0
      val apple = appleDomain.company
      apple mustNotEqual null
      apple.name mustEqual "Apple"
      apple.id.get must be > 0
    }
    "query partial and only persist partial" in {
      val steve = person.query(select(person.id, person.date) from person where person.name === "Steve Jobs").toList.head
      val newDate = System.currentTimeMillis()
      person.persist(steve.copy(date = newDate))
      val updated = person.query(person.q where person.name === "Steve Jobs").toList.head
      updated.name mustEqual "Steve Jobs"
      updated.date mustEqual newDate
    }
  }
  "SimpleInstance" should {
    "insert a single record without a 'modified' value and get a 'modified' value back" in {
      val original = SimpleInstance("test")
      original.modified mustEqual 0L
      val current = System.currentTimeMillis()
      val record = simple.persist(original)
      record.modified must beGreaterThanOrEqualTo(current)
    }
    "query the record back out with a modified value" in {
      val record = simple.query(simple.q).toList.head
      record.modified mustNotEqual 0L
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val person = new ORMTable[Person]("person") {
    val id = Column[Int]("id", PrimaryKey, AutoIncrement)
    val name = Column[String]("name", Unique)
    val date = Column[Long]("date")
  }
  val company = new ORMTable[Company]("company") {
    val id = Column[Int]("id", PrimaryKey, AutoIncrement)
    val name = Column[String]("name", Unique)
    val ownerId = Column[Int]("ownerId", new ForeignKey(person.id))
  }
  val domain = new ORMTable[CorporateDomain]("domain") {
    val id = Column[Int]("id", PrimaryKey, AutoIncrement)
    val url = Column[String]("url", Unique)
    val companyId = Column[Int]("companyId", new ForeignKey(company.id))
  }
  val simple = new ORMTable[SimpleInstance]("simple") with ModifiedSupport[SimpleInstance] {
    val id = Column[Int]("id", PrimaryKey, AutoIncrement)
    val name = Column[String]("name")
    val modified = Column[Long]("modified", NotNull)
  }

  person.map("companies", company.ownerId)
}

case class Person(name: String, date: Long = System.currentTimeMillis(), companies: LazyList[Company] = LazyList.Empty, id: Option[Int] = None)

case class Company(name: String, owner: Lazy[Person] = Lazy.None, id: Option[Int] = None)

case class CorporateDomain(url: String, company: Company, id: Option[Int] = None)

case class SimpleInstance(name: String, modified: Long = 0L, id: Option[Int] = None)