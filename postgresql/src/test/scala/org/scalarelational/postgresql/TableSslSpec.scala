package org.scalarelational.postgresql

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatastore, AbstractTableSpec, AbstractTestCrossReferenceDatastore, AbstractTestDatastore}

import scala.language.postfixOps

/**
 * @author Robert Djubek <envy1988@gmail.com>
 */
class TableSslSpec extends AbstractTableSpec {
  override def testDatastore = TestDatastoreSsl
  override def specialTypes = SpecialTypesDatastoreSsl
  override def testCrossReference = TestCrossReferenceDatastoreSsl
}

object TestDatastoreSsl extends PostgreSQLDatastore(PostgreSQL.Config("localhost", "tablespec", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractTestDatastore with HikariSupport
object TestCrossReferenceDatastoreSsl extends PostgreSQLDatastore(PostgreSQL.Config("localhost", "cross_reference", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractTestCrossReferenceDatastore
object SpecialTypesDatastoreSsl extends PostgreSQLDatastore(PostgreSQL.Config("localhost", "special_types", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractSpecialTypesDatastore
