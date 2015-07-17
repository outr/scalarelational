package org.scalarelational.h2

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractTableSpec, AbstractTestDatastore, AbstractTestCrossReferenceDatastore, AbstractSpecialTypesDatastore}
import org.scalarelational.model._
import scala.language.postfixOps

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TableSpec extends AbstractTableSpec {
  override def testDatastore = TestDatastore
  override def specialTypes = SpecialTypesDatastore
  override def testCrossReference = TestCrossReferenceDatastore
}

object TestDatastore extends H2Datastore(mode = H2Memory("tablespec")) with AbstractTestDatastore with HikariSupport
object TestCrossReferenceDatastore extends H2Datastore(mode = H2Memory("cross_reference")) with AbstractTestCrossReferenceDatastore
object SpecialTypesDatastore extends H2Datastore(mode = H2Memory("special_types")) with AbstractSpecialTypesDatastore