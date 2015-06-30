package org.scalarelational.model2

import org.scalarelational.model2.query.Select
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ModelSpec extends WordSpec with Matchers {
  object t1 extends Table("t1") {
    val id = Column[Int](this, "id")
    val name = Column[String](this, "name")
    val age = Column[Int](this, "age")
  }
  object t2 extends Table("t2") {
    val id = Column[Int](this, "id")
    val name = Column[String](this, "name")
    val t1Id = Column[Int](this, "t1Id")
  }

  "Model" when {
    "creating SQL selects" should {
      "handle a simple single column query" in {
        val q = select(t1.id) from t1
        val sql = q.toSQL
        sql.text should equal("SELECT(t1.id) FROM t1")
      }
      "handle a simple double column query" in {
        val q = select(t1.id, t1.name) from t1
        val sql = q.toSQL
        sql.text should equal("SELECT(t1.id, t1.name) FROM t1")
      }
      "handle a simple triple column query" in {
        val q = select(t1.id, t1.name, t1.age) from t1
        val sql = q.toSQL
        sql.text should equal("SELECT(t1.id, t1.name, t1.age) FROM t1")
      }
      "handle a simple alias column query" in {
        val q = select(t1.name as "test1") from t1
        val sql = q.toSQL
        sql.text should equal("SELECT(t1.name AS [test1]) FROM t1")
      }
      "handle a simple alias table query" in {
        val q = select(t1.name) from t1 as "table1"
        val sql = q.toSQL
        sql.text should equal("(SELECT(t1.name) FROM t1 AS [table1])")
      }
      "handle a simple sub-select query" in {
        val q1 = select(t1.id, t1.name, t1.age) from t1 as "table1"
        val q2 = select(q1(t1.name), q1(t1.age), t2.name) from t2 innerJoin q1 on q1(t1.id) === t2.t1Id
        val sql = q2.toSQL
        sql.text should equal("SELECT(table1.name, table1.age, t2.name) FROM t2 INNER JOIN (SELECT(t1.id, t1.name, t1.age) FROM t1 AS [table1]) ON table1.id = t2.t1Id")
      }
    }
  }

  def select[T1](f1: Field[T1]) = new Select(Group1[Field[_], Field[T1]](f1))
  def select[T1, T2](f1: Field[T1], f2: Field[T2]) = new Select(Group2[Field[_], Field[T1], Field[T2]](f1, f2))
  def select[T1, T2, T3](f1: Field[T1], f2: Field[T2], f3: Field[T3]) = new Select(Group3[Field[_], Field[T1], Field[T2], Field[T3]](f1, f2, f3))
}