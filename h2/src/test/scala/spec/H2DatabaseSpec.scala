package spec

import org.scalarelational.column.Column
import org.scalarelational.h2.H2Database
import org.scalarelational.h2.column.types.{DecimalType, IntType, VarCharType}
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}

class H2DatabaseSpec extends WordSpec with Matchers {
  import CoffeeHouseDatabase._

  "Tables" when {
    "using CoffeeHouseDatabase" should {
      "define non-null suppliers" in {
        Option(suppliers) shouldNot be(None)
      }
      "define non-null coffees" in {
        Option(coffees) shouldNot be(None)
      }
      "define suppliers.id with `supId`" in {
        suppliers.id.name should be("supId")
      }
      "define suppliers.name" in {
        suppliers.name.name should be("name")
      }
      "define coffees.id with `cofId`" in {
        coffees.id.name should be("cofId")
      }
    }
  }
}

object CoffeeHouseDatabase extends H2Database {
  val suppliers: Suppliers = table[Suppliers]()
  val coffees: Coffees = table[Coffees]()
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