package org.scalarelational

import org.scalarelational.column.property.{ForeignKey, PrimaryKey, AutoIncrement}
import org.scalarelational.model.{Table, SQLDatastore}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ModelSpec extends WordSpec with Matchers {
  import TestDatastore._

  "Model" when {
    "checking hierarchical structure" should {
      "have a table reference from the column" in {
        t1.name.table should equal(t1)
      }
    }
    "creating SQL selects" should {
      "handle a simple single column query" in {
        val q = select(t1.id) from t1
        val (sql, args) = describe(q)
        sql should equal("SELECT t1.id FROM t1")
        args should equal(Nil)
      }
      "handle a simple double column query" in {
        val q = select(t1.id, t1.name) from t1
        val (sql, args) = describe(q)
        sql should equal("SELECT t1.id, t1.name FROM t1")
        args should equal(Nil)
      }
      "handle a simple triple column query" in {
        val q = select(t1.id, t1.name, t1.age) from t1
        val (sql, args) = describe(q)
        sql should equal("SELECT t1.id, t1.name, t1.age FROM t1")
        args should equal(Nil)
      }
      "handle a simple alias column query" in {
        val q = select(t1.name as "test1") from t1
        val (sql, args) = describe(q)
        sql should equal("SELECT t1.name AS [test1] FROM t1")
        args should equal(Nil)
      }
      "handle a simple alias table query" in {
        val q = select(t1.name) from t1 as "table1"
        val (sql, args) = describe(q)
        sql should equal("SELECT t1.name FROM t1")
        args should equal(Nil)
      }
      "handle a simple sub-select query" in {
        val q1 = select(t1.id, t1.name, t1.age) from t1 as "table1"
        val q2 = select(q1(t1.name), q1(t1.age), t2.name) from t2 innerJoin q1 on q1(t1.id) === t2.t1Fk
        val (sql, args) = describe(q2)
        sql should equal("SELECT table1.name, table1.age, t2.name FROM t2 INNER JOIN (SELECT t1.id, t1.name, t1.age FROM t1) AS table1 ON table1.id = t2.t1Fk")
        args should equal(Nil)
      }
    }
  }
}

object TestDatastore extends SQLDatastore {
  object t1 extends Table("t1") {
    val id = column[Int]("id", AutoIncrement, PrimaryKey)
    val name = column[String]("name")
    val age = column[Int]("age")
  }
  object t2 extends Table("t2") {
    val id = column[Int]("id", AutoIncrement, PrimaryKey)
    val name = column[String]("name")
    val t1Fk = column[Int]("t1Fk", new ForeignKey(t1.id))
  }
}