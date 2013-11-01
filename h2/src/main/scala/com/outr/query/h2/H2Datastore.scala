package com.outr.query.h2

import com.outr.query.{Column, Table, Datastore}
import org.h2.jdbcx.JdbcConnectionPool
import org.powerscala.reflect.EnhancedClass

/**
 * @author Matt Hicks <matt@outr.com>
 */
class H2Datastore protected(mode: H2ConnectionMode = H2Memory(),
                            user: String = "sa",
                            password: String = "sa") extends Datastore {
  Class.forName("org.h2.Driver")

  val dataSource = JdbcConnectionPool.create(mode.url, user, password)

  def create(ifNotExist: Boolean, tables: Table*) = {
    val s = session
    val statement = s.connection.createStatement()
    tables.foreach(t => statement.execute(createTableSQL(ifNotExist, t)))
  }

  def createTableSQL(ifNotExist: Boolean, table: Table) = {
    val b = new StringBuilder

    b.append("CREATE TABLE ")
    if (ifNotExist) {
      b.append("IF NOT EXISTS ")
    }
    b.append(table.tableName)
    b.append('(')
    b.append(table.columns.map(c => column2SQL(c)).mkString(", "))
    b.append(')')

    b.toString()
  }

  def column2SQL(column: Column[_]) = {
    val b = new StringBuilder
    b.append(column.name)
    b.append(' ')
    b.append(columnType(column.manifest.runtimeClass))
    b.toString()
  }

  def columnType(c: Class[_]) = EnhancedClass.convertClass(c) match {
    case "Int" => "INTEGER"
    case "Long" => "BIGINT"
    case "String" => s"VARCHAR(${Int.MaxValue})"
    case classType => throw new RuntimeException(s"Unsupported column-type: $classType ($c).")
  }
}