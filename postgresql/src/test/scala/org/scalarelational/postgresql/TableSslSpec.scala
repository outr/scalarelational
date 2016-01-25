package org.scalarelational.postgresql

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatabase, AbstractTableSpec, AbstractTestCrossReferenceDatabase, AbstractTestDatabase}

import scala.language.postfixOps

/**
 * @author Robert Djubek <envy1988@gmail.com>
 */
class TableSslSpec extends AbstractTableSpec {
  override def testDatastore = TestDatabaseSsl$
  override def specialTypes = SpecialTypesDatabaseSsl$
  override def testCrossReference = TestCrossReferenceDatabaseSsl$
}

object TestDatabaseSsl$ extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "tablespec", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractTestDatabase with HikariSupport
object TestCrossReferenceDatabaseSsl$ extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "cross_reference", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractTestCrossReferenceDatabase
object SpecialTypesDatabaseSsl$ extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "special_types", "travis", "pa", ssl = Some(PostgreSQL.SSL(sslFactory = Some("org.postgresql.ssl.NonValidatingFactory"))))) with AbstractSpecialTypesDatabase
