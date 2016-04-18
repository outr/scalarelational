package spec

import javax.sql.DataSource

import org.scalarelational.Database
import org.scalarelational.dsl.ddl._
import org.scalatest.{Matchers, WordSpec}

class CreateTableSpec extends WordSpec with Matchers {
  "Create Table" should {
    "represent a simple table" in {
      val ddl = db.create.table("TEST")(db.create.int("ID").primaryKey, db.create.varchar("NAME", length = 255))
      ddl.describe should be("CREATE TABLE TEST(ID INTEGER PRIMARY KEY, NAME VARCHAR(255))")
    }
    "represent test_table" in {
      val expected = "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, date TIMESTAMP)"
      val ddl = db.create.table("test_table", ifNotExists = true)(
        db.create.int("id").autoIncrement.primaryKey,
        db.create.varchar("name", length = 200).notNull.unique,
        db.create.timestamp("date")
      )
      ddl.describe should be(expected)
    }
  }

  object db extends Database {
    override def dataSource: DataSource = None.orNull
  }
}