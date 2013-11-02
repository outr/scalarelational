package com.outr.query.h2

import com.outr.query.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object TestDatastore extends H2Datastore {
  val test = TestTable
  import test._

  def main(args: Array[String]): Unit = {
    println(createTableSQL(true, TestTable))
    create()

    val id = insert(test.name("Matt Hicks")).toList.head
    println(s"ID: $id")

    val results = exec(select(*).from(test))
    results.foreach {
      case r => println(s"Query Result: ${r(test.id)} - ${r(test.name)} - ${r(test.date)}")
    }
  }
}

object TestTable extends Table("test") {
  val id = Column[Int]("id", primaryKey = true, autoIncrement = true)
  val name = Column[String]("name", unique = true)
  val date = Column[Long]("date", default = Some(System.currentTimeMillis()))
}