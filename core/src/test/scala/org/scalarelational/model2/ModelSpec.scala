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
    }
  }

  def select[T1](f1: Field[T1]) = new Select(Group1[Field[_], Field[T1]](f1))
  def select[T1, T2](f1: Field[T1], f2: Field[T2]) = new Select(Group2[Field[_], Field[T1], Field[T2]](f1, f2))
  def select[T1, T2, T3](f1: Field[T1], f2: Field[T2], f3: Field[T3]) = new Select(Group3[Field[_], Field[T1], Field[T2], Field[T3]](f1, f2, f3))
}