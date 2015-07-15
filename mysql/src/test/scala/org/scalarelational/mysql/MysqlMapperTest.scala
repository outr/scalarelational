package org.scalarelational.mysql

import org.scalarelational.mapper._
import org.scalarelational.mysql.MysqlTestDataStore.suppliers
import org.scalarelational.mysql.MysqlTestDataStore._



object MysqlMapperTest extends App {

  override def main(args: Array[String]) {

    session {
      val starbucks = Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", "CA", "93966")
      val updated = suppliers.persist(starbucks).result

      val query = select(coffees.* ::: suppliers.*) from coffees innerJoin suppliers on(coffees.supID.opt === suppliers.id.opt) where(coffees.name === "French Roast")
      val (frenchRoast, superior) = query.to[Coffee, Supplier](coffees, suppliers).result.head()
      println(s"Coffee: $frenchRoast, Supplier: $superior")
    }
  }
}