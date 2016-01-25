package org.scalarelational.h2

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatabase, AbstractTableSpec, AbstractTestCrossReferenceDatabase, AbstractTestDatabase}

import scala.language.postfixOps

class TableSpec extends AbstractTableSpec {
  override def testDatastore: AbstractTestDatabase = TestDatabase
  override def specialTypes: AbstractSpecialTypesDatabase = SpecialTypesDatabase
  override def testCrossReference: AbstractTestCrossReferenceDatabase = TestCrossReferenceDatabase
}

object TestDatabase extends H2Database(mode = H2Memory("tablespec")) with AbstractTestDatabase with HikariSupport
object SpecialTypesDatabase extends H2Database(mode = H2Memory("special_types")) with AbstractSpecialTypesDatabase
object TestCrossReferenceDatabase extends H2Database(mode = H2Memory("cross_reference")) with AbstractTestCrossReferenceDatabase