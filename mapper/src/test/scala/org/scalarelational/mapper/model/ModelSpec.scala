package org.scalarelational.mapper.model

import java.sql.Timestamp

import org.scalarelational.column.Column
import org.scalarelational.column.property.{AutoIncrement, ForeignKey, PrimaryKey}
import org.scalarelational.model.SQLDatastore
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

/**
  * @author Matt Hicks <matt@outr.com>
  */
class ModelSpec extends WordSpec with Matchers {
  import ModelDatastore._

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
        sql should equal("SELECT t1.name AS test1 FROM t1")
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
    "checking Macro created table" should {
      "have 'PERSON' as the table name" in {
        person.tableName should equal("PERSON")
      }
      "have exactly four columns" in {
        person.columns.size should equal(4)
      }
      "have id: Option[Int] as the first column" in {
        val c: Column[_, _] = person.columns.head
        c.name should equal("ID")
        c.fieldName should equal("id")
        c.optional should equal(true)
        c.has(AutoIncrement) should equal(true)
        c.has(PrimaryKey) should equal(true)
      }
      "have name: String as the second column" in {
        val c: Column[_, _] = person.columns.tail.head
        c.name should equal("NAME")
        c.fieldName should equal("name")
        c.optional should equal(false)
        c.has(AutoIncrement) should equal(false)
        c.has(PrimaryKey) should equal(false)
      }
      "have age: Int as the third column" in {
        val c: Column[_, _] = person.columns.tail.tail.head
        c.name should equal("AGE")
        c.fieldName should equal("age")
        c.optional should equal(false)
      }
      "have created: Timestamp as the fourth column" in {
        val c: Column[_, _] = person.columns.tail.tail.tail.head
        c.name should equal("created")
        c.fieldName should equal("created")
        c.optional should equal(false)
      }
      "verify type access" in {
        person.id.name should equal("ID")
        person.name.name should equal("NAME")
        person.age.name should equal("AGE")
        person.created.name should equal("created")
      }
    }
  }
}

object ModelDatastore extends SQLDatastore {
  override protected def catalog: Option[String] = None

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
  object person extends Table("PERSON") {
    val id = column[Option[Int], Int]("ID", AutoIncrement, PrimaryKey)
    val name = column[String]("NAME")
    val age = column[Int]("AGE")
    val created = column[Timestamp]("created")
  }
  // TODO: fix @typedTable to properly create: val id = column[Option[Int], Int]
  /*@typedTable[Person] object person {
    id.props(AutoIncrement, PrimaryKey)
    val created = column[Timestamp]("created")
  }*/
}

case class Person(id: Option[Int], name: String, age: Int)