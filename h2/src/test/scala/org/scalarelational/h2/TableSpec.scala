package org.scalarelational.h2

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatastore, AbstractTableSpec, AbstractTestCrossReferenceDatastore, AbstractTestDatastore}

import scala.language.postfixOps

class TableSpec extends AbstractTableSpec {
  override def testDatastore: AbstractTestDatastore = TestDatastore
  override def specialTypes: AbstractSpecialTypesDatastore = SpecialTypesDatastore
  override def testCrossReference: AbstractTestCrossReferenceDatastore = TestCrossReferenceDatastore
}

object TestDatastore extends H2Datastore(mode = H2Memory("tablespec")) with AbstractTestDatastore with HikariSupport
object SpecialTypesDatastore extends H2Datastore(mode = H2Memory("special_types")) with AbstractSpecialTypesDatastore
object TestCrossReferenceDatastore extends H2Datastore(mode = H2Memory("cross_reference")) with AbstractTestCrossReferenceDatastore