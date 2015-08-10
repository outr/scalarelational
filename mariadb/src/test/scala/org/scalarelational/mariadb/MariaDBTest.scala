package org.scalarelational.mariadb

import org.scalarelational.mariadb.MariaDBTestDataStore$._
import org.scalarelational.mariadb.MariaDBTestDataStore$.suppliers._
import org.scalarelational.mariadb.MariaDBTestDataStore$.{coffees, suppliers}

// TODO: remove this as soon as the TableSpec is working
@Deprecated
object MariaDBTest extends App {

  override def main(args: Array[String]) {
    session {
      create(suppliers, coffees)

      //Create suppliers
      val acmeId = MariaDBTestDataStore$.insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).result
      val superiorCoffeeId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).result
      val theHighGroundId = insertInto(suppliers, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").result

      import org.scalarelational.mariadb.MariaDBTestDataStore$.coffees._

      session {
        // Batch insert some coffees
        insert(name("Colombian"), supID(acmeId), price(7.99), sales(0), total(0)).
          and(name("French Roast"), supID(superiorCoffeeId), price(8.99), sales(0), total(0)).
          and(name("Espresso"), supID(theHighGroundId), price(9.99), sales(0), total(0)).
          and(name("Colombian Decaf"), supID(acmeId), price(8.99), sales(0), total(0)).
          and(name("French Roast Decaf"), supID(superiorCoffeeId), price(9.99), sales(0), total(0)).result
      }

      val query = select(*) from coffees
      System.out.println(query.result)
      query.result.foreach {
        case r => println(s"  ${r(name)}\t${r(supID)}\t${r(price)}\t${r(sales)}\t${r(total)}")

      }
    }
  }
}
