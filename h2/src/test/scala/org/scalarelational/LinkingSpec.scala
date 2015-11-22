package org.scalarelational

import org.scalarelational.column.property.{AutoIncrement, IgnoreCase, PrimaryKey, Unique}
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.model.SQLLogging
import org.scalarelational.table.{LinkingTable, Table}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
class LinkingSpec extends WordSpec with Matchers {
  import LinkingDatastore._

  "LinkingSpec" should {
    "create tables" in {
      withSession { implicit session =>
        create(
          Content,
          Tag,
          ContentTagLinking
        )
      }
    }

    "insert rows" in {
      withSession { implicit session =>
        insert(Content.title("content")).result should equal (1)
        insert(Content.title("content2")).result should equal (2)
        insert(Content.title("content3")).result should equal (3)

        insert(Tag.name("tag")).result should equal (1)
        insert(Tag.name("tag2")).result should equal (2)

        def l = ContentTagLinking
        insert(l.contentId(1), l.tagId(2)).result should equal(1)
        insert(l.contentId(2), l.tagId(1)).result should equal(2)
        insert(l.contentId(3), l.tagId(1)).result should equal(3)
      }
    }

    "query content" in {
      withSession { implicit session =>
        val result =
          (Tag.q
            innerJoin ContentTagLinking on ContentTagLinking.tagId.opt === Tag.id
            where ContentTagLinking.contentId === 1
          ).result.toList

        result.size should equal (1)
        result.head(Tag.name) should equal ("tag2")
      }
    }
  }
}

object LinkingDatastore extends H2Datastore(mode = H2Memory("linking_test")) with SQLLogging {


  object Content extends Table("Content") {
    val id = column[Option[Int], Int]("id", AutoIncrement, PrimaryKey)
    val title = column[String]("title")
  }

  object Tag extends Table("Tag") {
    val id = column[Option[Int], Int]("id", AutoIncrement, PrimaryKey)
    val name = column[String]("name", Unique, IgnoreCase)
  }

  object ContentTagLinking extends LinkingTable("ContentTagLinking", Content.id, Tag.id) {
    def contentId = left
    def tagId = right
  }
}
