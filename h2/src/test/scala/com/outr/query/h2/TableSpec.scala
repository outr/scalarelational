package com.outr.query.h2

import org.specs2.mutable._
import com.outr.query._
import java.sql.SQLException

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends Specification {
  "TestTable" should {
    "have two columns" in {
      TestTable.columns must have size 3
    }
    "verify the create table String is correct" in {
      val sql = TestDatastore.createTableSQL(ifNotExist = true, TestTable)
      sql mustEqual "CREATE TABLE IF NOT EXISTS test(id INTEGER, name VARCHAR(2147483647), date BIGINT)"
    }
    "create the table" in {
      TestDatastore.create(ifNotExist = false, TestTable) must not(throwA[SQLException])
    }
    "create a simple query" in {
      import TestTable._
      val q = select(id, name).from(TestTable)
      q.columns must have size 2
    }
  }
}

object TestDatastore extends H2Datastore {
  val test = TestTable
}

object TestTable extends Table("test") {
  val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
  val name = Column[String]("name", unique = true)
  val date = Column[Long]("date", default = Some(System.currentTimeMillis()))
}