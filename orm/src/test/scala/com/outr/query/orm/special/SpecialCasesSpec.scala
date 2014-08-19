package com.outr.query.orm.special

import com.outr.query.Table
import com.outr.query.column.property._
import com.outr.query.h2.{H2Memory, H2Datastore}
import com.outr.query.orm.{ORMTable, LazyList}
import com.outr.query.table.property.{Index, Linking}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SpecialCasesSpec extends WordSpec with Matchers {
  "SpecialCases" should {
    "create the tables" in {
      SpecialCasesDatastore.create()
    }
    "create three tags" in {
      Tag.persist(Tag("Apple"))
      Tag.persist(Tag("Orange"))
      Tag.persist(Tag("Banana"))
    }
    "create one content with Apple and Orange" in {
      val apple = Tag.query(Tag.q where Tag.name === "Apple").head
      val orange = Tag.query(Tag.q where Tag.name === "Orange").head
      Content.persist(Content("Fruit Tray", LazyList(apple, orange)))
    }
    "verify Apple and Orange are part of the content" in {
      val contents = Content.query().toList
      contents.size should equal(1)
      val content = contents.head
      content.name should equal("Fruit Tray")
      content.tags().map(t => t.name).toSet should equal(Set("Apple", "Orange"))
    }
    "update the content with Orange and Banana" in {
      val orange = Tag.query(Tag.q where Tag.name === "Orange").head
      val banana = Tag.query(Tag.q where Tag.name === "Banana").head
      val content = Content.query().head
      val updated = content.copy(tags = LazyList(orange, banana))
      Content.persist(updated)
    }
    "verify Orange and Banana are part of the content" in {
      val contents = Content.query().toList
      contents.size should equal(1)
      val content = contents.head
      content.name should equal("Fruit Tray")
      content.tags().map(t => t.name).toSet should equal(Set("Orange", "Banana"))
    }
  }
}

object SpecialCasesDatastore extends H2Datastore(mode = H2Memory("special_cases")) {
  def content = Content

  def tag = Tag

  def contentTags = ContentTags

  LazyList.connect[Content, Tag, Int](content, "tags", contentTags.tagId, tag, null, contentTags.contentId)
}

case class Content(name: String, tags: LazyList[Tag], id: Option[Int] = None)

object Content extends ORMTable[Content](SpecialCasesDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
}

case class Tag(name: String, id: Option[Int] = None)

object Tag extends ORMTable[Tag](SpecialCasesDatastore) {
  val id = orm[Int, Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = orm[String]("name", Unique)
}

object ContentTags extends Table(SpecialCasesDatastore, Linking) {
  val id = column[Int]("id", AutoIncrement, PrimaryKey)
  val contentId = column[Int]("contentId", NotNull, new ForeignKey(Content.id))
  val tagId = column[Int]("tagId", NotNull, new ForeignKey(Tag.id))

  props(Index.unique("uniqueContentTags", contentId, tagId))
}