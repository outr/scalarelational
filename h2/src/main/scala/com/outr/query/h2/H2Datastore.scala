package com.outr.query.h2

import com.outr.query._
import org.h2.jdbcx.JdbcConnectionPool
import org.powerscala.reflect.EnhancedClass
import com.outr.query.Column
import com.outr.query.Insert
import java.sql.Statement

/**
 * @author Matt Hicks <matt@outr.com>
 */
class H2Datastore protected(mode: H2ConnectionMode = H2Memory(),
                            user: String = "sa",
                            password: String = "sa") extends Datastore {
  Class.forName("org.h2.Driver")

  val dataSource = JdbcConnectionPool.create(mode.url, user, password)

  def createTableSQL(ifNotExist: Boolean, table: Table) = {
    val b = new StringBuilder

    b.append("CREATE TABLE ")
    if (ifNotExist) {
      b.append("IF NOT EXISTS ")
    }
    b.append(table.tableName)
    b.append('(')
    b.append(table.columns.map(c => column2SQL(c)).mkString(", "))

    val primaryKeys = table.columns.collect {
      case c if c.primaryKey => c.name
    }
    if (primaryKeys.nonEmpty) {
      b.append(s", PRIMARY KEY(${primaryKeys.mkString(", ")})")
    }

    b.append(')')

    b.toString()
  }

  def column2SQL(column: Column[_]) = {
    val b = new StringBuilder
    b.append(column.name)
    b.append(' ')
    b.append(columnType(column.manifest.runtimeClass))
    if (column.notNull) {
      b.append(" NOT NULL")
    }
    if (column.autoIncrement) {
      b.append(" AUTO_INCREMENT")
    }
    if (column.unique) {
      b.append(" UNIQUE")
    }
    b.toString()
  }

  def columnType(c: Class[_]) = EnhancedClass.convertClass(c) match {
    case "Int" => "INTEGER"
    case "Long" => "BIGINT"
    case "String" => s"VARCHAR(${Int.MaxValue})"
    case classType => throw new RuntimeException(s"Unsupported column-type: $classType ($c).")
  }

  def value2SQLValue[T](cv: ColumnValue[T]) = cv.value
  def sqlValue2Value[T](c: Column[T], value: Any) = value

  def exec(query: Query) = {
    val columns = query.columns.map(c => s"${c.table.tableName}.${c.name}").mkString(", ")
    val sql = s"SELECT $columns FROM ${query.table.tableName}"
    val ps = session.connection.prepareStatement(sql)
    val resultSet = ps.executeQuery()
    new QueryResultsIterator(resultSet, query)
  }

  def exec(insert: Insert) = {
    val table = insert.values.head.column.table
    val columnNames = insert.values.map(cv => cv.column.name).mkString(", ")
    val columnValues = insert.values.map(cv => value2SQLValue(cv))
    val placeholder = columnValues.map(v => "?").mkString(", ")
    val insertString = s"INSERT INTO ${table.tableName} ($columnNames) VALUES($placeholder)"
    val ps = session.connection.prepareStatement(insertString, Statement.RETURN_GENERATED_KEYS)
    columnValues.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }
    ps.executeUpdate()
    val keys = ps.getGeneratedKeys
    new GeneratedKeysIterator(keys)
  }
}