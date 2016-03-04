package spec

import org.scalarelational.dsl.ddl._
import org.scalatest.{Matchers, WordSpec}

class CreateTableSpec extends WordSpec with Matchers {
  "Create Table" should {
    "represent a simple table" in {
      val ddl = create.table("TEST")("ID".int.primaryKey, "NAME".varchar(255))
      ddl.describe should be("CREATE TABLE TEST(ID INTEGER PRIMARY KEY, NAME VARCHAR(255))")
    }
    "represent test_table" in {
      val expected = "CREATE TABLE IF NOT EXISTS test_table(id INTEGER AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, date TIMESTAMP)"
      val ddl = create.table("test_table", ifNotExists = true)("id".int.autoIncrement.primaryKey, "name".varchar(200).notNull.unique, "date".timestamp)
      ddl.describe should be(expected)
    }
  }
}