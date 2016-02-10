package spec

import org.scalarelational.dsl.ddl._
import org.scalatest.{Matchers, WordSpec}

class CreateTableSpec extends WordSpec with Matchers {
  "Create Table" should {
    "represent a simple table" in {
      val ddl = create.table("TEST")("ID".int.primaryKey, "NAME".varchar(255))
      ddl.describe should be("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255))")
    }
  }
}