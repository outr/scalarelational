package org.scalarelational.mariadb

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatastore, AbstractTableSpec, AbstractTestCrossReferenceDatastore, AbstractTestDatastore}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends AbstractTableSpec {
  override def testDatastore = TestDatastore
  override def testCrossReference = TestCrossReferenceDatastore
  override def specialTypes = SpecialTypesDatastore
}

object TestDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "tablespec", "travis", "")) with AbstractTestDatastore with HikariSupport
object TestCrossReferenceDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "cross_reference", "travis", "")) with AbstractTestCrossReferenceDatastore
object SpecialTypesDatastore extends MariaDBDatastore(MariaDBConfig("localhost", "special_types", "travis", "")) with AbstractSpecialTypesDatastore
