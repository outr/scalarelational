package com.outr.query

import org.specs2.mutable._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification {
  "TestTable" should {
    "have two columns" in {
      TestTable.columns must have size 2
    }
  }
}

object TestTable extends Table {
  val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
  val name = Column[String]("name", unique = true)
}