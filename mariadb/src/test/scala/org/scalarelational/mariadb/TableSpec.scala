package org.scalarelational.mariadb

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatabase, AbstractTableSpec, AbstractTestCrossReferenceDatabase, AbstractTestDatabase}


class TableSpec extends AbstractTableSpec {
  override def testDatastore = TestDatabase
  override def testCrossReference = TestCrossReferenceDatabase
  override def specialTypes = SpecialTypesDatabase
}

object TestDatabase extends MariaDBDatabase(MariaDBConfig("localhost", "tablespec", "travis", "")) with AbstractTestDatabase with HikariSupport
object TestCrossReferenceDatabase extends MariaDBDatabase(MariaDBConfig("localhost", "cross_reference", "travis", "")) with AbstractTestCrossReferenceDatabase
object SpecialTypesDatabase extends MariaDBDatabase(MariaDBConfig("localhost", "special_types", "travis", "")) with AbstractSpecialTypesDatabase