package org.scalarelational.mariadb

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatastore, AbstractTableSpec, AbstractTestCrossReferenceDatastore, AbstractTestDatastore, Fruit}

class TableSpec extends AbstractTableSpec {
  override def testDatastore = TestDatastore
  override def testCrossReference = TestCrossReferenceDatastore
  override def specialTypes = SpecialTypesDatastore

  "SQL functions" should {
    val ds = testDatastore
    import ds._

    "call Now()" in {
      withSession { implicit session =>
        val x = ds.select(Now()).query.converted.head
        x.getTime should not equal(0)
      }
    }
    "call UnixTimestamp()" in {
      withSession { implicit session =>
        val x = ds.select(UnixTimestamp()).query.converted.head
        x should not equal(0)
      }
    }
  }
}

object TestDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "tablespec", "travis", "", serverTimezone = Some("UTC"))) with AbstractTestDatastore with HikariSupport

object TestCrossReferenceDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "cross_reference", "travis", "", serverTimezone = Some("UTC"))) with AbstractTestCrossReferenceDatastore

object SpecialTypesDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "special_types", "travis", "", serverTimezone = Some("UTC"))) with AbstractSpecialTypesDatastore