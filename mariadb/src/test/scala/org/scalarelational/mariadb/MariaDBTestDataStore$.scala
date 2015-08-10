package org.scalarelational.mariadb

import org.powerscala.log.Level

import org.scalarelational.column.property.{ColumnLength, PrimaryKey, AutoIncrement, ForeignKey}
import org.scalarelational.datatype.Ref
import org.scalarelational.model.SQLLogging
import org.scalarelational.table.Table

case class Supplier(name: String, street: String, city: String, state: String, zip: String, id: Option[Int] = None)
case class Coffee(name: String, supID : Option[Int], price: Double, sales: Int, total : Int , id: Option[Int])

// TODO: remove this as soon as the TableSpec is working
@Deprecated
object MariaDBTestDataStore$ extends MariaDBDatastore(MariaDBConfig("localhost","test", "user", "password") ) with SQLLogging{
  sqlLogLevel := Level.Info

  object suppliers extends Table("SUPPLIERS") {
    val name = column[String]("SUP_NAME", ColumnLength(100))
    val street = column[String]("STREET", ColumnLength(100))
    val city = column[String]("CITY", ColumnLength(100))
    val state = column[String]("STATE", ColumnLength(100))
    val zip = column[String]("ZIP", ColumnLength(100))
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
  }

  object coffees extends Table("COFFEES") {
    val name = column[String]("COF_NAME", ColumnLength(100))
    val supID = column[Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
    val id = column[Int]("COF_ID", PrimaryKey, AutoIncrement)
  }

}
