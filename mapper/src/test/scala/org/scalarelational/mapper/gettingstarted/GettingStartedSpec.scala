package org.scalarelational.mapper.gettingstarted

import enumeratum._
import org.scalarelational.column.property.{AutoIncrement, ForeignKey, PrimaryKey, Unique}
import org.scalarelational.datatype.Ref
import org.scalarelational.h2.{H2Database, H2Memory}
import org.scalarelational.mapper._
import org.scalarelational.result.QueryResult
import org.scalatest.{Matchers, WordSpec}

class GettingStartedSpec extends WordSpec with Matchers {
  import GettingStartedDatabase._

  var acmeId: Int = _
  var superiorCoffeeId: Int = _
  var theHighGroundId: Int = _

  "H2 examples" should {
    "Create your Database" in {
      withSession { implicit session =>
        create(suppliers, coffees)
      }
    }
    "Insert some Suppliers" in {
      import suppliers._

      transaction { implicit session =>
        // Clean and type-safe inserts
        acmeId = insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state(Some("CA")), status(Status.Unconfirmed), zip("95199")).result
        superiorCoffeeId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), zip("95460"), status(Status.Unconfirmed)).result

        // Short-hand when using values in order - we exclude the id since it will be generated by the database
        theHighGroundId = insertInto(suppliers, "The High Ground", "100 Coffee Lane", "Meadows", Some("CA"), "93966", Status.Unconfirmed).result
      }
    }
    "Validate Supplier IDs" in {
      acmeId should equal (1)
      superiorCoffeeId should equal (2)
      theHighGroundId should equal (3)
    }
    "Batch Insert some Coffees" in {
      import coffees._

      withSession { implicit session =>
        // Batch insert some coffees
        insert(name("Colombian"), supID(Ref[Supplier](acmeId)), price(7.99), sales(0), total(0)).
          and(name("French Roast"), supID(Ref[Supplier](superiorCoffeeId)), price(8.99), sales(0), total(0)).
          and(name("Espresso"), supID(Ref[Supplier](theHighGroundId)), price(9.99), sales(0), total(0)).
          and(name("Colombian Decaf"), supID(Ref[Supplier](acmeId)), price(8.99), sales(0), total(0)).
          and(name("French Roast Decaf"), supID(Ref[Supplier](superiorCoffeeId)), price(9.99), sales(0), total(0)).result
      }
    }
    "Query all the Coffees" in {
      import coffees._

      withSession { implicit session =>
        val results = (select(*) from coffees).result.toVector
        results.length should equal(5)
        check(results(0), "COFFEES(COF_NAME: Colombian, SUP_ID: 1, PRICE: 7.99, SALES: 0, TOTAL: 0, COF_ID: Some(1))")
        check(results(1), "COFFEES(COF_NAME: French Roast, SUP_ID: 2, PRICE: 8.99, SALES: 0, TOTAL: 0, COF_ID: Some(2))")
        check(results(2), "COFFEES(COF_NAME: Espresso, SUP_ID: 3, PRICE: 9.99, SALES: 0, TOTAL: 0, COF_ID: Some(3))")
        check(results(3), "COFFEES(COF_NAME: Colombian Decaf, SUP_ID: 1, PRICE: 8.99, SALES: 0, TOTAL: 0, COF_ID: Some(4))")
        check(results(4), "COFFEES(COF_NAME: French Roast Decaf, SUP_ID: 2, PRICE: 9.99, SALES: 0, TOTAL: 0, COF_ID: Some(5))")
      }

      def check(result: QueryResult, expected: String) = {
        val s = result.toString
        s should equal(expected)
      }
    }
    "Query all the Coffees explicitly" in {
      import GettingStartedDatabase.{coffees => c}

      withSession { implicit session =>
        val query = select (c.name, c.supID, c.price, c.sales, c.total) from coffees

        query.converted.map {
          case (name, supID, price, sales, total) => s"$name  $supID  $price  $sales  $total"
        }.mkString("\n")
      }
    }
    "Query all Coffees filtering and joining with Suppliers" in {
      withSession { implicit session =>
        val query = (
          select (coffees.name, suppliers.name)
            from coffees
            innerJoin suppliers
            on coffees.supID === suppliers.ref
            where coffees.price < 9.0
        )
        val results = query.converted.toVector
        results.length should equal(3)
        results(0) should equal(("Colombian", "Acme, Inc."))
        results(1) should equal(("French Roast", "Superior Coffee"))
        results(2) should equal(("Colombian Decaf", "Acme, Inc."))
      }
    }
  }
  "Mapper Examples" should {

    "Persist a new Supplier" in {
      withSession { implicit session =>
        val starbucks = Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", Some("CA"), "93966", Status.Enabled)
        val result = starbucks.insert.result
        result.id should equal(4)
      }
    }
    "Query a Supplier back" in {
      withSession { implicit session =>
        import suppliers._

        val query = select (*) from suppliers where name === "Starbucks"
        val starbucks = query.to[Supplier](suppliers).converted.head
        starbucks should equal (Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", Some("CA"), "93966", Status.Enabled, Some(4)))
      }
    }
    "Query a Supplier back as a tuple" in {
      withSession { implicit session =>
        import suppliers._

        val query = select(name, street, city, state, zip, status, id) from suppliers where name === "Starbucks"
        val starbucksTuple = query.converted.head
        // Because we explicitly defined the expressions we want back we can extract the results as a type-safe Tuple.
        starbucksTuple should equal(("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", Some("CA"), "93966", Status.Enabled, Some(4)))
        // Use our modified Tuple to create an instance of the Supplier
        val starbucks = Supplier.tupled.apply(starbucksTuple)
        starbucks should equal(Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", Some("CA"), "93966", Status.Enabled, Some(4)))
      }
    }
    "Query a Supplier back as a Supplier explicitly" in {
      withSession { implicit session =>
        import suppliers._

        // Our original query
        val query = select(name, street, city, state, zip, status, id) from suppliers where name === "Starbucks"
        // We can map this in our Query to the end resulting type without all the mess using Query.map
        val updated = query.map(Supplier.tupled)
        val starbucks = updated.converted.head
        starbucks should equal(Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", Some("CA"), "93966", Status.Enabled, Some(4)))
      }
    }
    "Query 'French Roast' with 'Superior Coffee' for (Coffee, Supplier)" in {
      withSession { implicit session =>
        val query = select (coffees.* ::: suppliers.*) from coffees innerJoin suppliers on (coffees.supID === suppliers.ref) where coffees.name === "French Roast"
        val (frenchRoast, superior) = query.to[Coffee, Supplier](coffees, suppliers).converted.head
        frenchRoast should equal(Coffee("French Roast", superior.ref, 8.99, 0, 0, Some(2)))
        superior should equal(Supplier("Superior Coffee", "1 Party Place", "Mendocino", None, "95460", Status.Unconfirmed, Some(2)))
      }
    }
  }
}

sealed abstract class Status extends EnumEntry

object Status extends Enum[Status] {
  case object Unconfirmed extends Status
  case object Disabled extends Status
  case object Enabled extends Status

  val values = findValues.toVector
}

object GettingStartedDatabase extends H2Database(mode = H2Memory("getting_started")) {
  implicit def statusEnum: Enum[Status] = Status

  object suppliers extends MappedTable[Supplier]("SUPPLIERS") {
    val name = column[String]("SUP_NAME", Unique)
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[Option[String], String]("STATE")
    val zip = column[String]("ZIP")
    val status = column[Status, String]("STATUS", enum[Status])
    val id = column[Option[Int], Int]("SUP_ID", PrimaryKey, AutoIncrement)

    override def query = q.to[Supplier](this)
  }

  object coffees extends MappedTable[Coffee]("COFFEES") {
    val name = column[String]("COF_NAME", Unique)
    val supID = column[Ref[Supplier], Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
    val id = column[Option[Int], Int]("COF_ID", PrimaryKey, AutoIncrement)

    override def query = q.to[Coffee](this)
  }
}

case class Supplier(name: String,
                    street: String,
                    city: String,
                    state: Option[String],
                    zip: String,
                    status: Status,
                    id: Option[Int] = None) extends Entity[Supplier] {
  def columns = mapTo[Supplier](GettingStartedDatabase.suppliers)
}

case class Coffee(name: String,
                  supID: Ref[Supplier],
                  price: Double,
                  sales: Int,
                  total: Int,
                  id: Option[Int] = None) extends Entity[Coffee] {
  def columns = mapTo[Coffee](GettingStartedDatabase.coffees)
}