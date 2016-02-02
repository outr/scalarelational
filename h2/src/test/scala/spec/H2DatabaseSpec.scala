package spec

import org.scalarelational.column.Column
import org.scalarelational.h2.H2Database
import org.scalarelational.h2.column.types.{DecimalType, IntType, VarCharType}
import org.scalarelational.table.Table
import org.scalarelational.table.property.TableName
import org.scalatest.{Matchers, WordSpec}

class H2DatabaseSpec extends WordSpec with Matchers {
  import CoffeeHouseDatabase._

  "Tables" when {
    "validating generated tables" should {
      "define non-null suppliers" in {
        Option(suppliers) shouldNot be(None)
      }
      "verify suppliers table name" in {
        suppliers.prop(TableName) should be(Some(TableName("coffee_suppliers")))
      }
      "verify reference to database in suppliers table" in {
        suppliers.database should be(CoffeeHouseDatabase)
      }
      "define non-null coffees" in {
        Option(coffees) shouldNot be(None)
      }
      "verify coffees table name" in {
        coffees.prop(TableName) should be(Some(TableName("coffees")))
      }
      "verify reference to database in coffees table" in {
        coffees.database should be(CoffeeHouseDatabase)
      }
      "verify known tables" in {
        tables should be(Vector(suppliers, coffees))
      }
    }
    "checking column naming" should {
      "define suppliers.id with `supId`" in {
        suppliers.id.name should be("supId")
      }
      "define suppliers.name" in {
        suppliers.name.name should be("name")
      }
      "define suppliers.street" in {
        suppliers.street.name should be("street")
      }
      "define suppliers.city" in {
        suppliers.city.name should be("city")
      }
      "define suppliers.state" in {
        suppliers.state.name should be("state")
      }
      "define suppliers.zip" in {
        suppliers.zip.name should be("zip")
      }
      "define coffees.id with `cofId`" in {
        coffees.id.name should be("cofId")
      }
      "define coffees.name" in {
        coffees.name.name should be("name")
      }
      "define coffees.supFk" in {
        coffees.supFk.name should be("supFk")
      }
      "define coffees.price" in {
        coffees.price.name should be("price")
      }
      "define coffees.sales" in {
        coffees.sales.name should be("sales")
      }
      "define coffees.total" in {
        coffees.total.name should be("total")
      }
    }
  }
}

object CoffeeHouseDatabase extends H2Database {
  val suppliers: Suppliers = table[Suppliers]("coffee_suppliers")
  val coffees: Coffees = table[Coffees]("coffees")
}

trait Suppliers extends Table {
  val id: Column[Int] = IntType.name("supId").primaryKey.autoIncrement
  val name: Column[String] = VarCharType.unique.ignoreCase
  val street: Column[Option[String]] = VarCharType.optional
  val city: Column[Option[String]] = VarCharType.optional
  val state: Column[Option[String]] = VarCharType.optional
  val zip: Column[Option[String]] = VarCharType.optional
}

trait Coffees extends Table {
  val id: Column[Int] = IntType.name("cofId").primaryKey.autoIncrement
  val name: Column[String] = VarCharType.unique.ignoreCase
  val supFk: Column[Option[Int]] = IntType.foreignKey(CoffeeHouseDatabase.suppliers.id).optional
  val price: Column[BigDecimal] = DecimalType.default(0.0)
  val sales: Column[Int] = IntType.default(0)
  val total: Column[Int] = IntType.default(0)
}