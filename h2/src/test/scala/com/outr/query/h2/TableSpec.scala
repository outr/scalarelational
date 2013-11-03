package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification {
  import TestDatastore._

  "TestTable" should {
    "have two columns" in {
      TestTable.columns must have size 3
    }
    "verify the create table String is correct" in {
      val sql = TestDatastore.createTableSQL(ifNotExist = true, TestTable)
      sql mustEqual "CREATE TABLE IF NOT EXISTS test(id INTEGER AUTO_INCREMENT, name VARCHAR(2147483647) UNIQUE, date BIGINT, PRIMARY KEY (id))"
    }
    "create the table" in {
      TestDatastore.create(ifNotExist = false) must not(throwA[Throwable])
    }
    "insert a record" in {
      create()
      val id = insert(test.name("Matt Hicks")).toList.head
      id mustEqual 1
    }
    "create a simple query" in {
      val q = select(test.id, test.name).from(test)
      q.columns must have size 2
    }
  }
}

object TestDatastore extends H2Datastore(mode = H2Memory("test")) {
  val test = TestTable

  def main(args: Array[String]): Unit = {
    create()
    val id = insert(test.name("Matt Hicks")).toList.head
    println(s"ID: $id")
  }
}

object TestTable extends Table("test") {
  val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
  val name = Column[String]("name", unique = true)
  val date = Column[Long]("date", default = Some(System.currentTimeMillis()))
}