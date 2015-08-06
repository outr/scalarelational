package org.scalarelational.postgresql

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatastore, AbstractTableSpec, AbstractTestCrossReferenceDatastore, AbstractTestDatastore}

import scala.language.postfixOps

/**
 * @author Robert Djubek <envy1988@gmail.com>
 */
case object Port {
  val p: Int = 5432
}

class TableSslSpec extends AbstractTableSpec {
  override def testDatastore = TestDatastoreSsl
  override def specialTypes = SpecialTypesDatastoreSsl
  override def testCrossReference = TestCrossReferenceDatastoreSsl
}

object TestDatastoreSsl extends PostgreSQLDatastore(PGConfig("localhost", "tablespec", "travis", "pa", Port.p, PGUseSsl(sslfactory = Some("org.postgresql.ssl.NonValidatingFactory")))) with AbstractTestDatastore with HikariSupport
object TestCrossReferenceDatastoreSsl extends PostgreSQLDatastore(PGConfig("localhost", "cross_reference", "travis", "pa", useSsl = PGUseSsl(sslfactory = Some("org.postgresql.ssl.NonValidatingFactory")))) with AbstractTestCrossReferenceDatastore
object SpecialTypesDatastoreSsl extends PostgreSQLDatastore(PGConfig("localhost", "special_types", "travis", "pa", useSsl = PGUseSsl(sslfactory = Some("org.postgresql.ssl.NonValidatingFactory")))) with AbstractSpecialTypesDatastore
