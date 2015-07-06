package org.scalarelational

import org.scalarelational.column.property._
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.model.Table
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
  }
}

object InheritanceDatastore extends H2Datastore(mode = H2Memory("inheritance_test")) {
  class BaseTable(table: String) extends Table(table) {
    val id = column[Option[Int]]("id", AutoIncrement, PrimaryKey)
  }

  object Content extends BaseTable("Content") {
    val title = column[String]("title")
  }
}
