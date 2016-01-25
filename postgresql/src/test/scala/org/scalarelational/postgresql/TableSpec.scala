package org.scalarelational.postgresql

import org.scalarelational.extra.HikariSupport
import org.scalarelational.{AbstractSpecialTypesDatabase, AbstractTableSpec, AbstractTestCrossReferenceDatabase, AbstractTestDatabase}

import scala.language.postfixOps

/**
 * @author Robert Djubek <envy1988@gmail.com>
 */
class TableSpec extends AbstractTableSpec {
  override def testDatastore = TestDatabase
  override def specialTypes = SpecialTypesDatabase
  override def testCrossReference = TestCrossReferenceDatabase

  override protected def expectedNotNone = ("SELECT test_table.id FROM test_table WHERE test_table.id IS NOT NULL", Nil)
}

object TestDatabase extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "tablespec", "travis", "pa")) with AbstractTestDatabase with HikariSupport
object TestCrossReferenceDatabase extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "cross_reference", "travis", "pa")) with AbstractTestCrossReferenceDatabase
object SpecialTypesDatabase extends PostgreSQLDatabase(PostgreSQL.Config("localhost", "special_types", "travis", "pa")) with AbstractSpecialTypesDatabase
