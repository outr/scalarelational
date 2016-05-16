package spec

import javax.sql.DataSource

import org.scalarelational.Database
import org.scalarelational.column.params.{AutoIncrement, NotNull, PrimaryKey, Unique}
import org.scalarelational.column.types.{IntType, TimestampType, VarCharType}
import org.scalarelational.instruction.{ColumnDescriptor, CreateTable}
import org.scalatest.{Matchers, WordSpec}

class DDLSpec extends WordSpec with Matchers {
  object db extends Database {
    override def dataSource: DataSource = throw new UnsupportedOperationException("DataSource should not be referenced in DDLSpec.")
  }

  "DDL" should {
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

      val instruction = db.create.table("TEST")("ID".integer.primaryKey, "NAME".varchar(255))
      instruction.sql should equal(expected)
      instruction.args should equal(Vector.empty)
    }
    "create a slightly more complex table with DSL" in {
      import org.scalarelational.dsl._

      val expected = "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, date TIMESTAMP)"

      val instruction = db.create.table("test_table", ifNotExists = true)(
        "id".integer.autoIncrement.primaryKey,
        "name".varchar(200).notNull.unique,
        "date".timestamp
      )
      instruction.sql should equal(expected)
      instruction.args should equal(Vector.empty)
    }
  }
}