package org.scalarelational

import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.column.property.{IgnoreCase, PrimaryKey, Unique, AutoIncrement}
import org.scalarelational.table.{Table, LinkingTable}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
class LinkingSpec extends WordSpec with Matchers {
  import LinkingDatastore._

  "LinkingSpec" should {
    "create tables" in {
      session {
        create(
          Content,
          Tag,
          ContentTagLinking
        )
      }
    }

    "insert rows" in {
      session {
        insert(Content, Content.title("content")).result.id should equal (1)
        insert(Content, Content.title("content2")).result.id should equal (2)

        insert(Tag, Tag.name("tag")).result.id should equal (1)
        insert(Tag, Tag.name("tag2")).result.id should equal (2)

        insertInto(ContentTagLinking, 1, 2).result.id should equal (1)
        insertInto(ContentTagLinking, 2, 1).result.id should equal (2)
      }
    }

    "query content" in {
      session {
        val result =
          (Tag.q
            from Tag
            innerJoin ContentTagLinking on ContentTagLinking.tagId.opt === Tag.id
            where ContentTagLinking.contentId === 1
          ).result.toList

        result.size should equal (1)
        result.head(Tag.name) should equal ("tag2")
      }
    }
  }
}

object LinkingDatastore extends H2Datastore(mode = H2Memory("linking_test")) {
  object Content extends Table[Unit]("Content") {
    val id = column[Option[Int]]("id", AutoIncrement, PrimaryKey)
    val title = column[String]("title")
  }

  object Tag extends Table[Unit]("Tag") {
    val id = column[Option[Int]]("id", AutoIncrement, PrimaryKey)
    val name = column[String]("name", Unique, IgnoreCase)
  }

  object ContentTagLinking extends LinkingTable("ContentTagLinking", Content.id, Tag.id) {
    def contentId = left
    def tagId = right
  }
}
