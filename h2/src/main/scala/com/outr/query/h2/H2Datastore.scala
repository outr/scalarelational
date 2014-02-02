package com.outr.query.h2

import com.outr.query._
import org.h2.jdbcx.JdbcConnectionPool
import com.outr.query.Column
import java.sql.Statement
import java.io.NotSerializableException
import scala.collection.mutable.ListBuffer
import org.powerscala.log.Logging
import com.outr.query.property._
import com.outr.query.convert.ColumnConverter
import com.outr.query.Update
import com.outr.query.LikeCondition
import com.outr.query.Join
import com.outr.query.SimpleFunction
import scala.Some
import com.outr.query.Delete
import com.outr.query.DirectCondition
import com.outr.query.RangeCondition
import com.outr.query.ColumnCondition
import com.outr.query.Conditions
import com.outr.query.Merge
import com.outr.query.Query
import com.outr.query.Insert
import com.outr.query.table.property.Index

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class H2Datastore protected(val mode: H2ConnectionMode = H2Memory(),
                            val dbUser: String = "sa",
                            val dbPassword: String = "sa") extends Datastore with Logging {
  Class.forName("org.h2.Driver")

  val dataSource = JdbcConnectionPool.create(mode.url, dbUser, dbPassword)

  def createTableSQL(ifNotExist: Boolean, table: Table) = {
    val b = new StringBuilder

    b.append("CREATE TABLE ")
    if (ifNotExist) {
      b.append("IF NOT EXISTS ")
    }
    b.append(table.tableName)
    b.append('(')
    b.append(table.columns.map(c => column2SQL(c)).mkString(", "))

    if (table.primaryKeys.nonEmpty) {
      b.append(s", PRIMARY KEY(${table.primaryKeys.map(c => c.name).mkString(", ")})")
    }

    b.append(");")

    b.toString()
  }

  def createTableReferences(table: Table) = {
    val b = new StringBuilder

    table.foreignKeys.foreach {
      case c => {
        val foreignKey = ForeignKey(c).foreignColumn
        b.append(s"ALTER TABLE ${table.tableName}\r\n")
        b.append(s"\tADD FOREIGN KEY(${c.name})\r\n")
        b.append(s"\tREFERENCES ${foreignKey.table.tableName} (${foreignKey.name});\r\n\r\n")
      }
    }

    b.toString()
  }

  def createTableIndexes(table: Table) = {
    val b = new StringBuilder

    table.columns.foreach {
      case c => c.get[Indexed](Indexed.name) match {
        case Some(index) => {
          b.append(s"CREATE INDEX IF NOT EXISTS ${index.indexName} ON ${table.tableName}(${c.name});\r\n\r\n")
        }
        case None => // No index on this column
      }
    }

    table.properties.foreach {
      case index: Index => b.append(s"CREATE ${if (index.unique) "UNIQUE " else ""}INDEX IF NOT EXISTS ${index.indexName} ON ${table.tableName}(${index.columns.map(c => c.name).mkString(", ")});\r\n\r\n")
      case _ => // Ignore other table properties
    }

    b.toString()
  }

  def column2SQL(column: Column[_]) = {
    val b = new StringBuilder
    b.append(column.name)
    b.append(' ')
    b.append(column.sqlType)
    if (column.has(NotNull)) {
      b.append(" NOT NULL")
    }
    if (column.has(AutoIncrement)) {
      b.append(" AUTO_INCREMENT")
    }
    if (column.has(Unique)) {
      b.append(" UNIQUE")
    }
    b.toString()
  }

  private def expression2SQL(expression: SelectExpression) = expression match {
    case c: ColumnLike[_] => c.longName
    case f: SimpleFunction[_] => s"${f.functionType.name.toUpperCase}(${f.column.longName})"
  }

  def sqlFromQuery(query: Query) = {
    val columns = query.expressions.map(expression2SQL).mkString(", ")

    var args = List.empty[Any]

    // Generate SQL
    val (joins, joinArgs) = joins2SQL(query.joins)
    args = args ::: joinArgs
    val (where, whereArgs) = where2SQL(query.whereCondition)
    args = args ::: whereArgs
    val groupBy = if (query._groupBy.nonEmpty) {
      s" GROUP BY ${query._groupBy.map(expression2SQL).mkString(", ")}"
    } else {
      ""
    }
    val orderBy = if (query._orderBy.nonEmpty) {
      s" ORDER BY ${query._orderBy.map(ob => s"${expression2SQL(ob.expression)} ${ob.direction.sql}").mkString(", ")}"
    } else {
      ""
    }
    val limit = if (query._limit != -1) {
      s" LIMIT ${query._limit}"
    } else {
      ""
    }
    val offset = if (query._offset != -1) {
      s" OFFSET ${query._offset}"
    } else {
      ""
    }
    s"SELECT $columns FROM ${query.table.tableName}$joins$where$groupBy$orderBy$limit$offset" -> args
  }

  def exec(query: Query) = active {
    val (sql, args) = sqlFromQuery(query)

//    info(s"$sql - ${args.mkString(", ")}")
    val ps = session.connection.prepareStatement(sql)

    // Populate args
    args.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }

    val resultSet = ps.executeQuery()
    new QueryResultsIterator(resultSet, query)
  }

  def exec(insert: Insert) = active {
    val table = insert.values.head.column.table
    val columnNames = insert.values.map(cv => cv.column.name).mkString(", ")
    val columnValues = insert.values.map(cv => cv.toSQL)
    val placeholder = columnValues.map(v => "?").mkString(", ")
    val insertString = s"INSERT INTO ${table.tableName} ($columnNames) VALUES($placeholder)"
//    info(s"$insertString - ${columnValues.mkString(", ")}")
    val ps = session.connection.prepareStatement(insertString, Statement.RETURN_GENERATED_KEYS)
    columnValues.zipWithIndex.foreach {
      case (value, index) => try {
        ps.setObject(index + 1, value)
      } catch {
        case exc: NotSerializableException => throw new RuntimeException(s"Index: $index (zero-based) is not serializable for insert($columnNames)", exc)
        case t: Throwable => throw new RuntimeException(s"Index: $index (zero-based) - $value failed for insert($columnNames)", t)
      }
    }
    ps.executeUpdate()
    val keys = ps.getGeneratedKeys
    new GeneratedKeysIterator(keys)
  }
  
  def exec(merge: Merge) = active {
    val table = merge.key.table
    val columnNames = merge.values.map(cv => cv.column.name).mkString(", ")
    val columnValues = merge.values.map(cv => cv.toSQL)
    val placeholder = columnValues.map(v => "?").mkString(", ")
    val mergeString = s"MERGE INTO ${table.tableName} ($columnNames) KEY(${merge.key.name}) VALUES($placeholder)"
    val ps = session.connection.prepareStatement(mergeString)
    columnValues.zipWithIndex.foreach {
      case (value, index) => try {
        ps.setObject(index + 1, value)
      } catch {
        case exc: NotSerializableException => throw new RuntimeException(s"Index: $index (zero-based) is not serializable for insert($columnNames)", exc)
      }
    }
    ps.executeUpdate()
  }

  def exec(update: Update) = active {
    var args = List.empty[Any]
    val sets = update.values.map(cv => s"${cv.column.longName}=?").mkString(", ")
    val setArgs = update.values.map(cv => cv.toSQL)
    args = args ::: setArgs

    val (where, whereArgs) = where2SQL(update.whereCondition)
    args = args ::: whereArgs
    val sql = s"UPDATE ${update.table.tableName} SET $sets$where"
    val ps = session.connection.prepareStatement(sql)

    // Populate args
    args.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }

    ps.executeUpdate()
  }

  def exec(delete: Delete) = active {
    var args = List.empty[Any]

    val (where, whereArgs) = where2SQL(delete.whereCondition)
    args = args ::: whereArgs
    val sql = s"DELETE FROM ${delete.table.tableName}$where"
    val ps = session.connection.prepareStatement(sql)

    // Populate args
    args.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }

    ps.executeUpdate()
  }

  def condition2String(condition: Condition, args: ListBuffer[Any]): String = condition match {
    case c: ColumnCondition[_] => {
      s"${c.column.longName} ${c.operator.symbol} ${c.other.longName}"
    }
    case c: NullCondition[_] => {
      s"${c.column.longName} ${c.operator.symbol} NULL"
    }
    case c: DirectCondition[_] => {
      args += c.column.converter.asInstanceOf[ColumnConverter[Any]].toSQLType(c.column.asInstanceOf[Column[Any]], c.value)
      s"${c.column.longName} ${c.operator.symbol} ?"
    }
    case c: LikeCondition[_] => {
      args += c.pattern
      s"${c.column.longName} ${if (c.not) "NOT " else ""}LIKE ?"
    }
    case c: RegexCondition[_] => {
      args += c.regex.toString()
      s"${c.column.longName} ${if (c.not) "NOT " else ""}REGEXP ?"
    }
    case c: RangeCondition[_] => throw new UnsupportedOperationException("RangeCondition isn't supported yet!")
    case c: Conditions => {
      val sql = c.list.map(condition => condition2String(condition, args)).mkString(s" ${c.connectType.name.toUpperCase} ")
      s"($sql)"
    }
  }

  private def joins2SQL(joins: List[Join]): (String, List[Any]) = {
    val args = ListBuffer.empty[Any]

    val b = new StringBuilder
    joins.foreach {
      case join => {
        val pre = join.joinType match {
          case JoinType.Inner => " INNER JOIN "
          case JoinType.Join => " JOIN "
          case JoinType.Left => " LEFT JOIN "
          case JoinType.LeftOuter => " LEFT OUTER JOIN "
        }
        b.append(pre)
        b.append(join.table.tableName)
        if (join.alias != null) {
          b.append(s" AS ${join.alias}")
        }
        b.append(" ON ")
        b.append(condition2String(join.condition, args))
      }
    }

    (b.toString(), args.toList)
  }

  private def where2SQL(condition: Condition): (String, List[Any]) = if (condition != null) {
    val args = ListBuffer.empty[Any]
    val sql = condition2String(condition, args)
    if (sql != null && sql.nonEmpty) {
      s" WHERE $sql" -> args.toList
    } else {
      "" -> Nil
    }
  } else {
    "" -> Nil
  }
}