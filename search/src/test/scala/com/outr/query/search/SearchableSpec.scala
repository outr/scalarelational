package com.outr.query.search

import org.apache.lucene.facet.FacetField
import org.scalatest.{Matchers, WordSpec}
import com.outr.query.h2.H2Datastore
import com.outr.query.Table
import com.outr.query.column.property.{NotNull, Unique, AutoIncrement, PrimaryKey}
import org.powerscala.search.{DocumentUpdate, Search}
import org.apache.lucene.document._
import com.outr.query.orm.ORMTable
import com.outr.query.h2.trigger.TriggerEvent
import com.outr.query.h2.H2Memory
import org.apache.lucene.index.Term

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SearchableSpec extends WordSpec with Matchers {
  import TestDatastore._

  "Searchable" should {
    "create the tables" in {
      create()
    }
    "add some basic entries to the datastore" in {
      insert(test.name("first"), test.date(System.currentTimeMillis()), test.tags("count,one,test"))
      insert(test.name("second"), test.date(System.currentTimeMillis()), test.tags("count,two,test"))
      insert(test.name("third"), test.date(System.currentTimeMillis()), test.tags("count,three,test"))
    }
    "search to find the entries in the index" in {
      val results = TestDatastore.search.query.run()
      results.total should equal(3)
    }
    "delete a basic entry from the datastore" in {
      exec(delete(test) where test.id === 1)
    }
    "search to find one fewer entries in the index" in {
      val results = TestDatastore.search.query.run()
      results.total should equal(2)
    }
    "add a couple users into the datastore" in {
      user.persist(User("John Doe", 30))
      user.persist(User("Jane Doe", 28))
      user.persist(User("Baby Doe", 3))
    }
    "search to find both test and user entries in the index" in {
      val results = TestDatastore.search.query.run()
      results.total should equal(5)
    }
    "search to find only test entries in the index" in {
      val results = TestDatastore.search.query("type:test").run()
      results.total should equal(2)
    }
    "search to find only user entries in the index" in {
      val results = TestDatastore.search.query("type:user").run()
      results.total should equal(3)
    }
    "search to find only user entries with the name starting with Jane" in {
      val results = TestDatastore.search.query.term("user", "type").prefix("Jane", "name").run()
      results.total should equal(1)
      results.docs.head.get("name") should equal("Jane Doe")
    }
    "add a user into datastore in a transaction" in {
      transaction {
        user.persist(User("Another Doe", 40))
      }
      val results = TestDatastore.search.query.term("user", "type").prefix("Another", "name").run()
      results.total should equal(1)
    }
    "add a user into datastore in a transaction and rollback" in {
      an [RuntimeException] should be thrownBy transaction {
        transaction {
          user.persist(User("Temporary Doe", 50))
          throw new RuntimeException
        }
      }
      val results = TestDatastore.search.query.term("user", "type").prefix("Temporary", "name").run()
      results.total should equal(0)
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) with SearchSupport {
  val search = new Search("fullText")
  search.facetsConfig.setMultiValued("tag", true)

  override def delayedCommit = false      // For testing purposes we want immediate commits

  override protected def createSearchForTable(table: Table) = search

  def test = TestTable
  def user = User
}

object TestTable extends Table(TestDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", Unique)
  val date = column[Long]("date")
  val tags = column[String]("tags")

  props(TestTableSearchable)
}

case class User(name: String, age: Int, id: Option[Int] = None)

object User extends ORMTable[User](TestDatastore, UserSearchable) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
  val age = orm[Int]("age", NotNull)
}

object TestTableSearchable extends BasicSearchable {
  override def event2DocumentUpdate(evt: TriggerEvent) = {
    val id = evt(TestTable.id)
    val name = evt(TestTable.name)
    val date = evt(TestTable.date)
    val tags = evt(TestTable.tags).split(",").toList
    val fullText = s"$id $name ${tags.mkString(" ")}"
    val fields = List(
      new StringField("docId", s"test$id", Field.Store.YES),
      new StringField("id", id.toString, Field.Store.YES),
      new StringField("name", name, Field.Store.YES),
      new LongField("date", date, Field.Store.YES),
      new TextField("fullText", fullText, Field.Store.NO),
      new StringField("type", "test", Field.Store.YES)
    ) ::: tags.map(t => new FacetField("tag", t))
    Some(DocumentUpdate(fields, Nil))
  }
}

object UserSearchable extends ORMSearchable(User) {
  override def toDocumentUpdate(u: User) = {
    Some(DocumentUpdate(
      new StringField("docId", s"user${u.id.get}", Field.Store.YES),
      new StringField("id", u.id.get.toString, Field.Store.YES),
      new StringField("name", u.name, Field.Store.YES),
      new IntField("age", u.age, Field.Store.YES),
      new TextField("fullText", s"${u.id.get} ${u.name} ${u.age}", Field.Store.NO),
      new StringField("type", "user", Field.Store.YES)
    ))
  }

  override def deleteDocument(evt: TriggerEvent) = Right(new Term("docId", s"user${evt(User.id)}"))
}