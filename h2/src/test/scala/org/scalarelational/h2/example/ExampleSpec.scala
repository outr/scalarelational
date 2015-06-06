package org.scalarelational.h2.example

import org.scalarelational.Table
import org.scalarelational.column.property.{AutoIncrement, ForeignKey, PrimaryKey}
import org.scalarelational.dsl._
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ExampleSpec extends WordSpec with Matchers {
  import ExampleDatastore._

  "example" should {
    "create the database" in {
      session {
        create(suppliers, coffees)
      }
    }
    "populate some data for suppliers" in {
      import suppliers._

      session {
        // Clean and type-safe inserts
        insert(id(101), name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).result
        insert(id(49), name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).result
        // Short-hand when using values in order
        insertInto(suppliers, 150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").result
      }
    }
    "populate some data for coffees" in {
      import coffees._

      session {
        // Batch insert some coffees
        insert(name("Colombian"), supID(101), price(7.99), sales(0), total(0)).
           add(name("French Roast"), supID(49), price(8.99), sales(0), total(0)).
           add(name("Espresso"), supID(150), price(9.99), sales(0), total(0)).
           add(name("Colombian Decaf"), supID(101), price(8.99), sales(0), total(0)).
           add(name("French Roast Decaf"), supID(49), price(9.99), sales(0), total(0)).result
      }
    }
    "query all coffees back" in {
      import coffees._

      session {
        println("Coffees:")
        (select(*) from coffees).result.foreach {
          case r => println(s"  ${r(name)}\t${r(supID)}\t${r(price)}\t${r(sales)}\t${r(total)}")
        }
      }
    }
    "query all coffees back filtering and joining with suppliers" in {
      session {
        println("Filtered Results:")
        (select(coffees.name, suppliers.name) from coffees innerJoin suppliers on coffees.supID === suppliers.id where coffees.price < 9.0).result.foreach {
          case r => println(s"  Coffee: ${r(coffees.name)}, Supplier: ${r(suppliers.name)}")
        }
      }
    }
  }
}

object ExampleDatastore extends H2Datastore(mode = H2Memory("example")) {
  object suppliers extends Table("SUPPLIERS") {
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
  }

  object coffees extends Table("COFFEES") {
    val name = column[String]("COF_NAME", PrimaryKey)
    val supID = column[Int]("SUP_ID", new ForeignKey(ExampleDatastore.suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
  }
}