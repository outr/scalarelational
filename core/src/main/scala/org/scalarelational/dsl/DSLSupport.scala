package org.scalarelational.dsl

import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.result.QueryResult
import org.scalarelational.SelectExpression
import org.scalarelational.instruction._
import org.scalarelational.table.Table

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DSLSupport {
  import DSLSupport._

  def select[E](e1: SelectExpression[E]) = {
    Query[SelectExpression[E], E](e1, converter = singleValueConverter[E])
  }
  def select[E1, E2](e1: SelectExpression[E1], e2: SelectExpression[E2]) = {
    Query[(SelectExpression[E1], SelectExpression[E2]), (E1, E2)]((e1, e2), converter = tuple2Converter[E1, E2])
  }
  def select[E1, E2, E3](e1: SelectExpression[E1], e2: SelectExpression[E2], e3: SelectExpression[E3]) = {
    Query[(SelectExpression[E1], SelectExpression[E2], SelectExpression[E3]), (E1, E2, E3)]((e1, e2, e3), converter = tuple3Converter[E1, E2, E3])
  }
  def select[E1, E2, E3, E4](e1: SelectExpression[E1], e2: SelectExpression[E2], e3: SelectExpression[E3], e4: SelectExpression[E4]) = {
    Query[(SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4]), (E1, E2, E3, E4)]((e1, e2, e3, e4), converter = tuple4Converter[E1, E2, E3, E4])
  }
  def select[E1, E2, E3, E4, E5](e1: SelectExpression[E1], e2: SelectExpression[E2], e3: SelectExpression[E3], e4: SelectExpression[E4], e5: SelectExpression[E5]) = {
    Query[(SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5]), (E1, E2, E3, E4, E5)]((e1, e2, e3, e4, e5), converter = tuple5Converter[E1, E2, E3, E4, E5])
  }
  def select[E1, E2, E3, E4, E5, E6](e1: SelectExpression[E1], e2: SelectExpression[E2], e3: SelectExpression[E3], e4: SelectExpression[E4], e5: SelectExpression[E5], e6: SelectExpression[E6]) = {
    Query[(SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5], SelectExpression[E6]), (E1, E2, E3, E4, E5, E6)]((e1, e2, e3, e4, e5, e6), converter = tuple6Converter[E1, E2, E3, E4, E5, E6])
  }
  def select[E1, E2, E3, E4, E5, E6, E7](e1: SelectExpression[E1], e2: SelectExpression[E2], e3: SelectExpression[E3], e4: SelectExpression[E4], e5: SelectExpression[E5], e6: SelectExpression[E6], e7: SelectExpression[E7]) = {
    Query[(SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5], SelectExpression[E6], SelectExpression[E7]), (E1, E2, E3, E4, E5, E6, E7)]((e1, e2, e3, e4, e5, e6, e7), converter = tuple7Converter[E1, E2, E3, E4, E5, E6, E7])
  }
  def select(expressions: List[SelectExpression[_]]) = Query[Vector[SelectExpression[_]], QueryResult[_]](expressions.toVector, converter = DefaultConverter)
  def insert(table: Table, values: ColumnValue[_]*) = InsertSingle[Int](table, values, identity[Int])
  def insertInto(table: Table, values: Any*) = insert(table, values.zip(table.columns).map {
    case (value, column) => column.asInstanceOf[Column[Any]](value)
  }: _*)
  def insertBatch[T](table: Table, rows: Seq[Seq[ColumnValue[_]]]) = InsertMultiple(table, rows)
  def merge[T](table: Table, key: Column[_], values: ColumnValue[_]*) = Merge(table, key, values.toList)
  def update[T](table: Table, values: ColumnValue[_]*) = Update[Int](table, values.toList, null, identity[Int])
  def delete[T](table: Table) = Delete(table)
}

object DSLSupport extends DSLSupport {
  val DefaultConverter = (qr: QueryResult[_]) => qr

  def singleValueConverter[E] = (qr: QueryResult[E]) => qr.values.head.value.asInstanceOf[E]
  implicit def vectorifySingleValue[E](t: SelectExpression[E]): Vector[SelectExpression[_]] = Vector(t)

  def tuple2Converter[E1, E2] = (qr: QueryResult[(E1, E2)]) => qr.values.head.value.asInstanceOf[E1] -> qr.values(1).value.asInstanceOf[E2]
  implicit def vectorifyTuple2[E1, E2](t: (SelectExpression[E1], SelectExpression[E2])): Vector[SelectExpression[_]] = Vector(t._1, t._2)

  def tuple3Converter[E1, E2, E3] = (qr: QueryResult[(E1, E2, E3)]) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3])
  implicit def vectorifyTuple3[E1, E2, E3](t: (SelectExpression[E1], SelectExpression[E2], SelectExpression[E3])): Vector[SelectExpression[_]] = Vector(t._1, t._2, t._3)

  def tuple4Converter[E1, E2, E3, E4] = (qr: QueryResult[(E1, E2, E3, E4)]) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3], qr.values(3).value.asInstanceOf[E4])
  implicit def vectorifyTuple4[E1, E2, E3, E4](t: (SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4])): Vector[SelectExpression[_]] = Vector(t._1, t._2, t._3, t._4)

  def tuple5Converter[E1, E2, E3, E4, E5] = (qr: QueryResult[(E1, E2, E3, E4, E5)]) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3], qr.values(3).value.asInstanceOf[E4], qr.values(4).value.asInstanceOf[E5])
  implicit def vectorifyTuple5[E1, E2, E3, E4, E5](t: (SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5])): Vector[SelectExpression[_]] = Vector(t._1, t._2, t._3, t._4, t._5)

  def tuple6Converter[E1, E2, E3, E4, E5, E6] = (qr: QueryResult[(E1, E2, E3, E4, E5, E6)]) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3], qr.values(3).value.asInstanceOf[E4], qr.values(4).value.asInstanceOf[E5], qr.values(5).value.asInstanceOf[E6])
  implicit def vectorifyTuple6[E1, E2, E3, E4, E5, E6](t: (SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5], SelectExpression[E6])): Vector[SelectExpression[_]] = Vector(t._1, t._2, t._3, t._4, t._5, t._6)

  def tuple7Converter[E1, E2, E3, E4, E5, E6, E7] = (qr: QueryResult[(E1, E2, E3, E4, E5, E6, E7)]) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3], qr.values(3).value.asInstanceOf[E4], qr.values(4).value.asInstanceOf[E5], qr.values(5).value.asInstanceOf[E6], qr.values(6).value.asInstanceOf[E7])
  implicit def vectorifyTuple7[E1, E2, E3, E4, E5, E6, E7](t: (SelectExpression[E1], SelectExpression[E2], SelectExpression[E3], SelectExpression[E4], SelectExpression[E5], SelectExpression[E6], SelectExpression[E7])): Vector[SelectExpression[_]] = Vector(t._1, t._2, t._3, t._4, t._5, t._6, t._7)
}