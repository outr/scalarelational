package spec

import org.scalatest.{Matchers, WordSpec}

class CreateTableSpec extends WordSpec with Matchers {
  "Create Table" should {
    "represent a simple table" in {
      val ddl = create table "simple" (column("id").int.primaryKey, column("name").varchar(255))
      // TODO: validate the resulting SQL
    }
  }
}
