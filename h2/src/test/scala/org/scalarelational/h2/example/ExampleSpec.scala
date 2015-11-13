package org.scalarelational.h2.example

import org.scalarelational.column.property.{ForeignKey, PrimaryKey}
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.table.Table
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
        insert(name("Colombian"), supID(101), price(7.99), sales(0), total(0), rating(Some(0.5))).
           and(name("French Roast"), supID(49), price(8.99), sales(0), total(0), rating(Some(0.3))).
           and(name("Espresso"), supID(150), price(9.99), sales(0), total(0), rating(None)).
           and(name("Colombian Decaf"), supID(101), price(8.99), sales(0), total(0), rating(Some(0.2))).
           and(name("French Roast Decaf"), supID(49), price(9.99), sales(0), total(0), rating(None)).result
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
    "query rating.avg" in {
      import coffees._
      session {
        (select (Avg(rating))
          from coffees
        ).result.converted.one.get should be > 0.0
      }
    }
    "query rating.avg without None" in {
      import coffees._
      session {
        (select (Avg(rating))
          from coffees
          where rating.!==(None) // !== conflicts with ScalaTest
        ).result.converted.one.get should be > 0.0
      }
    }
  }
}

object ExampleDatastore extends H2Datastore(mode = H2Memory("example")) {
  object suppliers extends Table("SUPPLIERS") {
    val id = column[Int]("SUP_ID", PrimaryKey)
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
    val rating = column[Option[Double], Double]("RATING")
    val total = column[Int]("TOTAL")
  }
}