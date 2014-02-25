package com.outr.query.search

import org.scalatest.{Matchers, WordSpec}
import com.outr.query.h2.{H2Memory, H2Datastore}
import com.outr.query.Table
import com.outr.query.column.property.{Unique, AutoIncrement, PrimaryKey}
import org.powerscala.search.{DocumentUpdate, Search}
import com.outr.query.h2.trigger.TriggerEvent
import org.apache.lucene.document.{TextField, LongField, Field, StringField}
import org.apache.lucene.facet.taxonomy.CategoryPath

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
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) with SearchSupport {
  val search = new Search("fullText")

  override def delayedCommit = false      // For testing purposes we want immediate commits

  override protected def createSearchForTable(table: Table) = search

  def test = TestTable
}

object TestTable extends Table(TestDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", Unique)
  val date = column[Long]("date")
  val tags = column[String]("tags")

  props(TestTableSearchable)
}

object TestTableSearchable extends BasicSearchable {
  override def event2DocumentUpdate(evt: TriggerEvent) = {
    val id = evt(TestTable.id)
    val name = evt(TestTable.name)
    val date = evt(TestTable.date)
    val tags = evt(TestTable.tags).split(",").toList
    val fullText = s"$id $name ${tags.mkString(" ")}"
    val fields = List(
      new StringField("id", id.toString, Field.Store.YES),
      new StringField("name", name, Field.Store.YES),
      new LongField("date", date, Field.Store.YES),
      new TextField("fullText", fullText, Field.Store.NO)
    )
    val paths = tags.map(t => new CategoryPath("tag", t))
    DocumentUpdate(fields, paths)
  }
}