package org.scalarelational

import org.scalarelational.column.property.{NotNull, PrimaryKey, AutoIncrement}
import org.scalarelational.model.{Table, SQLDatastore}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ModelSpec extends WordSpec with Matchers {

}

object TestDatastore extends SQLDatastore {
  object t1 extends Table("t1") {
    val id = column[Int]("id", AutoIncrement, PrimaryKey)
    val name = column[String]("name", NotNull)
    val age = column[Int]("age", NotNull)
  }
  object t2 extends Table("t2") {
    val id = column[Int]("id", AutoIncrement, PrimaryKey)
  }
}