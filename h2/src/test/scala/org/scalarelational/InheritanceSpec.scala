package org.scalarelational

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey}
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
class InheritanceSpec extends WordSpec with Matchers {
  import InheritanceDatastore._

  "InheritanceSpec" should {
    "create tables" in {
      session {
        create(
          Content
        )
      }
    }

    "insert rows" in {
      session {
        insert(Content.title("content")).result should equal (1)
        insert(Content.title("content2")).result should equal (2)
      }
    }

    "query content" in {
      session {
        (Content.q from Content where Content.id === Some(2))
          .result.hasNext should equal (true)
      }
    }

    "query content with mapper" in {
      session {
        val query = Content.q from Content where Content.id === Some(2)
        query.to[Content].result.converted.head should equal (Content("content2", Some(2)))
      }
    }
  }
}

object InheritanceDatastore extends H2Datastore(mode = H2Memory("inheritance_test")) {
  class BaseTable(table: String) extends Table(table) {
    val id = column[Option[Int], Int]("id", AutoIncrement, PrimaryKey)
  }

  object Content extends BaseTable("Content") {
    val title = column[String]("title")
  }

  trait Base {
    def id: Option[Int]
  }

  case class Content(title: String, id: Option[Int] = None) extends Base
}