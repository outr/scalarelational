package org.scalarelational.model

import java.io.File
import javax.sql.DataSource

import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, DataTypes, TypedValue}
import org.scalarelational.fun.SQLFunction
import org.scalarelational.instruction._
import org.scalarelational.instruction.ddl.BasicDDLSupport
import org.scalarelational.op._
import org.scalarelational.table.{Table, TableAlias}
import org.scalarelational.{SelectExpression, Session}
import pl.metastack.metarx.Opt

import scala.collection.mutable.ListBuffer

abstract class SQLDatabase protected() extends Database with BasicDDLSupport {
  protected def this(dataSource: DataSource) {
    this()
    dataSourceProperty := dataSource
  }

  val dataSourceProperty = Opt[DataSource]()

  def dataSource: Option[DataSource] = dataSourceProperty.get

  private def expression2SQL(expression: SelectExpression[_]) = expression match {
    case c: ColumnLike[_, _] => c.longName
    case f: SQLFunction[_, _] => f.alias match {
      case Some(alias) => s"${f.functionType.sql}(${f.column.longName}) AS $alias"
      case None => s"${f.functionType.sql}(${f.column.longName})"
    }
  }

  def describe[E, R](query: Query[E, R]): (String, List[TypedValue[_, _]]) = {
    val columns = query.expressions.vector.map(expression2SQL).mkString(", ")

    var args = List.empty[TypedValue[_, _]]

    // Generate SQL
    val (joins, joinArgs) = joins2SQL(query.joins)
    args = args ::: joinArgs
    val (where, whereArgs) = where2SQL(query.whereCondition)
    args = args ::: whereArgs
    val groupBy = if (query.grouping.nonEmpty) {
      s" GROUP BY ${query.grouping.map(expression2SQL).mkString(", ")}"
    } else {
      ""
    }
    val orderBy = if (query.ordering.nonEmpty) {
      s" ORDER BY ${query.ordering.map(ob => s"${expression2SQL(ob.expression)} ${ob.direction.sql}").mkString(", ")}"
    } else {
      ""
    }
    val limit = if (query.resultLimit != -1) {
      s" LIMIT ${query.resultLimit}"
    } else {
      ""
    }
    val offset = if (query.resultOffset != -1) {
      s" OFFSET ${query.resultOffset}"
    } else {
      ""
    }
    s"SELECT $columns FROM ${query.table.tableName}$joins$where$groupBy$orderBy$limit$offset" -> args
  }

  def exportTable(table: Table, file: File, drop: Boolean = true)
                 (implicit session: Session): Boolean = {
    val command = new StringBuilder("SCRIPT ")
    if (drop) {
      command.append("DROP ")
    }
    command.append("TO '")
    command.append(file.getCanonicalPath)
    command.append("' TABLE ")
    command.append(table.tableName)

    //    val command = s"SCRIPT TO '${file.getCanonicalPath}' TABLE ${table.tableName}"
    session.execute(command.toString())
  }

  def importScript(file: File)(implicit session: Session): Boolean = {
    val command = s"RUNSCRIPT FROM '${file.getCanonicalPath}'"
    session.execute(command)
  }

  protected def invoke[E, R](query: Query[E, R])(implicit session: Session) = {
    val (sql, args) = describe(query)
    SQLContainer.calling(query.table, InstructionType.Query, sql)

    session.executeQuery(sql, args)
  }

  protected def invoke[T](insert: InsertSingle[T])
                         (implicit session: Session): Int = {
    if (insert.values.isEmpty) throw new IndexOutOfBoundsException(s"Attempting an insert query with no values: $insert")
    val table = insert.table
    val columnNames = insert.values.map(_.column.name).mkString(", ")
    val columnValues = insert.values.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL))
    val placeholder = columnValues.map(v => "?").mkString(", ")
    val insertString = s"INSERT INTO ${table.tableName} ($columnNames) VALUES ($placeholder)"
    SQLContainer.calling(table, InstructionType.Insert, insertString)
    val resultSet = session.executeInsert(insertString, columnValues)
    try {
      if (resultSet.next()) {
        resultSet.getInt(1)  // TODO This restricts the PKs on PostgreSQL to integers
      } else {
        -1
      }
    } finally {
      resultSet.close()
    }
  }

  protected def invoke(merge: Merge)(implicit session: Session): Int = {
    val table = merge.key.table
    val columnNames = merge.values.map(_.column.name).mkString(", ")
    val columnValues = merge.values.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL))
    val placeholder = columnValues.map(v => "?").mkString(", ")
    val mergeString = s"MERGE INTO ${table.tableName} ($columnNames) KEY(${merge.key.name}) VALUES($placeholder)"
    SQLContainer.calling(table, InstructionType.Merge, mergeString)
    session.executeUpdate(mergeString, columnValues)
  }

  protected def invoke(insert: InsertMultiple)
                      (implicit session: Session): List[Int] = {
    if (insert.rows.isEmpty) throw new IndexOutOfBoundsException(s"Attempting a multi-insert with no values: $insert")
    if (insert.rows.tail.nonEmpty) {
      if (!insert.rows.map(_.length).sliding(2).forall {
        case Seq(first, second) => first == second
      }) {
        throw new IndexOutOfBoundsException(s"In multi-inserts all rows must have the exact same length.")
      }
    }
    val table = insert.rows.head.head.column.table
    val columnNames = insert.rows.head.map(_.column.name).mkString(", ")
    val columnValues = insert.rows.map(r => r.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL)))
    val placeholder = insert.rows.head.map(v => "?").mkString(", ")
    val insertString = s"INSERT INTO ${table.tableName} ($columnNames) VALUES($placeholder)"
    SQLContainer.calling(table, InstructionType.Insert, insertString)
    val resultSet = session.executeInsertMultiple(insertString, columnValues)
    try {
      val indexes = ListBuffer.empty[Int]
      while (resultSet.next()) {
        indexes += resultSet.getInt(1)
      }
      indexes.toList
    } finally {
      resultSet.close()
    }
  }

  protected def invoke[T](update: Update[T])(implicit session: Session): Int = {
    var args = List.empty[TypedValue[_, _]]
    val sets = update.values.map(cv => s"${cv.column.name}=?").mkString(", ")
    val setArgs = update.values.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL))
    args = args ::: setArgs

    val (where, whereArgs) = where2SQL(update.whereCondition)
    args = args ::: whereArgs
    val sql = s"UPDATE ${update.table.tableName} SET $sets$where"
    SQLContainer.calling(update.table, InstructionType.Update, sql)
    session.executeUpdate(sql, args)
  }

  protected def invoke(delete: Delete)(implicit session: Session) = {
    var args = List.empty[TypedValue[_, _]]

    val (where, whereArgs) = where2SQL(delete.whereCondition)
    args = args ::: whereArgs
    val sql = s"DELETE FROM ${delete.table.tableName}$where"
    SQLContainer.calling(delete.table, InstructionType.Delete, sql)
    session.executeUpdate(sql, args)
  }

  def condition2String(condition: Condition, args: ListBuffer[TypedValue[_, _]]): String = condition match {
    case c: ColumnCondition[_, _] => {
      s"${c.column.longName} ${c.operator.symbol} ${c.other.longName}"
    }
    case c: DirectCondition[_, _] => {
      val dataType = c.column.dataType.asInstanceOf[DataType[Any, Any]]
      val op = dataType.sqlOperator(c.column.asInstanceOf[ColumnLike[Any, Any]], c.value, c.operator)
      args += dataType.typed(dataType.converter.toSQL(c.column.asInstanceOf[ColumnLike[Any, Any]], c.value))
      s"${c.column.longName} ${op.symbol} ?"
    }
    case c: LikeCondition[_, _] => {
      args += DataTypes.StringType.typed(c.pattern)
      s"${c.column.longName} ${if (c.not) "NOT " else ""}LIKE ?"
    }
    case c: RegexCondition[_, _] => {
      args += DataTypes.StringType.typed(c.regex.toString())
      s"${c.column.longName} ${if (c.not) "NOT " else ""}REGEXP ?"
    }
    case c: RangeCondition[_, _] => {
      c.values.foreach {
        case v => {
          val dataType = c.column.dataType.asInstanceOf[DataType[Any, Any]]
          args += dataType.typed(dataType.converter.toSQL(c.column.asInstanceOf[ColumnLike[Any, Any]], v))
        }
      }
      val entries = c.operator match {
        case Operator.Between => c.values.map(v => "?").mkString(" AND ")
        case _ => c.values.map(v => "?").mkString("(", ", ", ")")
      }
      s"${c.column.longName} ${c.operator.symbol}$entries"
    }
    case c: Conditions => {
      val sql = c.list.map(condition => condition2String(condition, args)).mkString(s" ${c.connectType.entryName.toUpperCase} ")
      s"($sql)"
    }
  }

  private def joins2SQL(joins: List[Join]): (String, List[TypedValue[_, _]]) = {
    val args = ListBuffer.empty[TypedValue[_, _]]

    val b = new StringBuilder
    joins.foreach {
      case join => {
        val pre = join.joinType match {
          case JoinType.Inner => " INNER JOIN "
          case JoinType.Join => " JOIN "
          case JoinType.Left => " LEFT JOIN "
          case JoinType.LeftOuter => " LEFT OUTER JOIN "
          case JoinType.Outer => " OUTER JOIN "
        }
        b.append(pre)
        join.joinable match {
          case t: Table => b.append(t.tableName)
          case t: TableAlias => b.append(s"${t.table.tableName} AS ${t.tableAlias}")
          case q: Query[_, _] => {
            val (sql, queryArgs) = describe(q)
            b.append("(")
            b.append(sql)
            b.append(")")
            q.alias match {
              case Some(alias) => b.append(s" AS $alias")
              case None => // No alias assigned to the Query
            }
            args ++= queryArgs
          }
          case j => throw new RuntimeException(s"Unsupported Joinable: $j")
        }
        b.append(" ON ")
        b.append(condition2String(join.condition, args))
      }
    }

    (b.toString(), args.toList)
  }

  private def where2SQL(conditionOption: Option[Condition]) = conditionOption match {
    case Some(condition) => {
      val args = ListBuffer.empty[TypedValue[_, _]]
      Option(condition2String(condition, args)) match {
        case Some(sql) if sql.nonEmpty => {
          s" WHERE $sql" -> args.toList
        }
        case _ => "" -> Nil
      }
    }
    case None => "" -> Nil
  }

  override def dispose(): Unit = {
  }
}
