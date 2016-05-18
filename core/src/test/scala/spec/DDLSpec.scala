package spec

import javax.sql.DataSource

import org.scalarelational.Database
import org.scalarelational.column.params.{AutoIncrement, NotNull, PrimaryKey, Unique}
import org.scalarelational.column.types.{IntType, TimestampType, VarCharType}
import org.scalarelational.instruction.{ColumnDescriptor, CreateColumn, CreateTable}
import org.scalatest.{Matchers, WordSpec}

class DDLSpec extends WordSpec with Matchers {
  object db extends Database {
    override def dataSource: DataSource = throw new UnsupportedOperationException("DataSource should not be referenced in DDLSpec.")
  }

  "DDL" when {
    "creating tables" should {
      "create a simple table with `CreateTable`" in {
        val expected = "CREATE TABLE TEST(ID INTEGER PRIMARY KEY, NAME VARCHAR(255))"
        val id = ColumnDescriptor("ID", IntType, List(PrimaryKey))
        val name = ColumnDescriptor("NAME", VarCharType(255), Nil)
        val instruction = CreateTable(db, "TEST", false, id, name)
        instruction.sql should equal(expected)
        instruction.args should equal(Vector.empty)
      }
      "create slightly more complex table with `CreateTable`" in {
        val expected = "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, date TIMESTAMP)"
        val id = ColumnDescriptor("id", IntType, List(AutoIncrement, PrimaryKey))
        val name = ColumnDescriptor("name", VarCharType(200), List(NotNull, Unique))
        val date = ColumnDescriptor("date", TimestampType, Nil)
        val instruction = CreateTable(db, "test_table", true, id, name, date)
        instruction.sql should equal(expected)
        instruction.args should equal(Vector.empty)
      }
      "create a simple table with DSL" in {
        import org.scalarelational.dsl._

        val expected = "CREATE TABLE TEST(ID INTEGER PRIMARY KEY, NAME VARCHAR(255))"

        val instruction = db.create.table("TEST", integer("ID").primaryKey, varchar("NAME", 255))
        instruction.sql should equal(expected)
        instruction.args should equal(Vector.empty)
      }
      "create a slightly more complex table with DSL" in {
        import org.scalarelational.dsl._

        val expected = "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, date TIMESTAMP)"

        val instruction = db.create.tableIfNotExists(
          "test_table",
          integer("id").autoIncrement.primaryKey,
          varchar("name", 200).notNull.unique,
          timestamp("date")
        )
        instruction.sql should equal(expected)
        instruction.args should equal(Vector.empty)
      }
    }
    "creating columns" should {
      "create a simple column" in {
        val expected = "ALTER TABLE TEST ADD ID INTEGER"
        val id = ColumnDescriptor("ID", IntType, Nil)
        val instruction = CreateColumn(db, "TEST", id)
        instruction.sql should equal(expected)
        instruction.args should equal(Vector.empty)
      }
    }
  }
}