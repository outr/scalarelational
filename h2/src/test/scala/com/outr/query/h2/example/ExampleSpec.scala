package com.outr.query.h2.example

import com.outr.query.Table
import com.outr.query.column.property.{ForeignKey, AutoIncrement, PrimaryKey}
import com.outr.query.h2.{H2Memory, H2Datastore}
import org.scalatest.{Matchers, WordSpec}

import com.outr.query.simple._

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
      session {
        import suppliers._

        // Clean and type-safe inserts
        insert(id(101), name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199"))
        insert(id(49), name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460"))
        // Short-hand when using values in order
        insertInto(suppliers, 150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966")
      }
    }
    "populate some data for coffees" in {
      session {
        import coffees._

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