package com.outr.query.orm

import org.specs2.mutable._
import com.outr.query.h2.{H2Memory, H2Datastore}
import com.outr.query.Column

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
    // TODO: cross-reference
    // TODO: LazyList
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val person = new ORMTable[Person]("person") {
    val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
    val name = Column[String]("name", unique = true)
    val date = Column[Long]("date")
  }
  val company = new ORMTable[Company]("company") {
    val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
    val name = Column[String]("name", unique = true)
    val ownerId = Column[Int]("ownerId", foreignKey = Some(person.id))
  }
}

case class Person(name: String, date: Long = System.currentTimeMillis(), companies: LazyList[Company] = LazyList.Empty, id: Option[Int] = None)

case class Company(name: String, owner: Lazy[Person] = Lazy.None, id: Option[Int] = None)