package org.scalarelational.result

import java.io.{ByteArrayOutputStream, InputStream}
import java.sql.ResultSet
import javax.sql.rowset.serial.SerialBlob

import org.scalarelational.column.{ColumnLike, ColumnValue}
import org.scalarelational.datatype.{BlobSQLType, DataType}
import org.scalarelational.fun.{SQLFunction, SQLFunctionValue}
import org.scalarelational.instruction.Query
import org.scalarelational.{ExpressionValue, SelectExpression}

import scala.annotation.tailrec


class QueryResultsIterator[E, R](rs: ResultSet, val query: Query[E, R]) extends Iterator[QueryResult] {
  private val NextNotCalled = 0
  private val HasNext = 1
  private val NothingLeft = 2
  private var nextStatus = NextNotCalled

  def hasNext: Boolean = synchronized {
    if (nextStatus == NextNotCalled) {
      nextStatus = if (rs.next()) HasNext else NothingLeft
    }
    nextStatus == HasNext
  }

  def nextOption(): Option[QueryResult] = if (hasNext) {
    try {
      val values = query.expressions.vector.zipWithIndex.map {
        case (expression, index) => valueFromExpressions(expression, index)
      }
      Some(QueryResult(query.table, values))
    } finally {
      synchronized {
        nextStatus = NextNotCalled
      }
    }
  } else {
    None
  }

  def next(): QueryResult = nextOption().getOrElse(throw new RuntimeException("No more results. Use nextOption() instead."))

  private val bufferSize = 1024
  private def readInputStream(in: InputStream): ByteArrayOutputStream = {
    val out = new ByteArrayOutputStream()
    val buffer = new Array[Byte](bufferSize)
    @tailrec
    def read(): Unit = in.read(buffer) match {
      case -1 => // Finished
      case len => {
        out.write(buffer, 0, len)
        read()
      }
    }
    read()
    out
  }

  protected def columnValue[T, S](rs: ResultSet, index: Int, c: ColumnLike[T, S], dataType: DataType[T, S]): T = {
    val valueOption = Option(dataType.sqlType match {
      case t: BlobSQLType =>
        val binaryStream = rs.getBinaryStream(index + 1)
        val byteArrayStream = readInputStream(binaryStream)
        new SerialBlob(byteArrayStream.toByteArray)

      case _ => rs.getObject(index + 1)
    })

    def convert(value: AnyRef) = try {
      dataType.converter.fromSQL(c, value.asInstanceOf[S])
    } catch {
      case t: Throwable => {
        throw new RuntimeException(s"Error converting $value for column ${c.longName}. Query: ${query.table.datastore.describe(query)}", t)
      }
    }

    valueOption match {
      case None if !c.isOptional => None.orNull.asInstanceOf[T]
      case None => convert(None.orNull[AnyRef])
      case Some(value) => convert(value)
    }
  }

  protected def valueFromExpressions[T, S](expression: SelectExpression[T], index: Int): ExpressionValue[T] =
    expression match {
      case column: ColumnLike[_, _] => {
        val c = column.asInstanceOf[ColumnLike[T, S]]
        val value = columnValue[T, S](rs, index, c, c.dataType)
        ColumnValue[T, S](c, value, None)
      }
      case function: SQLFunction[_, _] => {
        val f = function.asInstanceOf[SQLFunction[T, S]]
        val value = columnValue[T, S](rs, index, f.column.asInstanceOf[ColumnLike[T, S]], f.converter)
        SQLFunctionValue[T, S](f, value)
      }
    }

  def one: QueryResult = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }

  def head: QueryResult = next()
  def headOption: Option[QueryResult] = nextOption()
}

class EnhancedIterator[T](iterator: Iterator[T]) extends Iterator[T] {
  override def hasNext: Boolean = iterator.hasNext

  override def next(): T = iterator.next()

  def nextOption(): Option[T] = if (hasNext) Option(next()) else None

  def head: T = next()
  def headOption: Option[T] = nextOption()

  def one: T = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }
}