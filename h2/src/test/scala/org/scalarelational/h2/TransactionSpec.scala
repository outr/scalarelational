package org.scalarelational.h2

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

class TransactionSpec extends WordSpec with Matchers {
  "Transaction" when {
    "nested" should {
      import StickyTransactionDatastore._
      withSession { implicit session =>
        create(fruit)
      }

      def trans(): Unit = transaction { implicit sess => }

      def insertApple(): Unit =
        withSession { implicit session =>
          trans()
          insert(fruit.name("apple")).result
        }

      def insertOrange(): Unit = {
        transaction { implicit sess => }
        withSession { implicit sess =>
          insert(fruit.name("orange")).result
        }
      }

      "insert 1" in {
        insertApple()
        withSession { implicit sess =>
          (select(fruit.name) from fruit where fruit.name === "apple").converted.toList.length should be(1)
        }
      }

      "insert 2" in {
        insertOrange()
        withSession { implicit sess =>
          (select(fruit.name) from fruit where fruit.name === "orange").converted.toList.length should be(1)
        }
      }
    }
  }
}

object TransactionDatastore extends H2Datastore {
  object fruit extends Table("FRUIT") {
    val name = column[String]("name", Unique)
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
  }
}

object StickyTransactionDatastore extends H2Datastore {
  object fruit extends Table("FRUIT") {
    val name = column[String]("name", Unique)
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
  }
}
