package org.scalarelational.dsl

import org.scalarelational.SelectExpression
import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.instruction._
import org.scalarelational.instruction.query._
import org.scalarelational.result.QueryResult
import org.scalarelational.table.Table

import scala.language.implicitConversions


trait DSLSupport {
  import DSLSupport._

  def select[E](e1: SelectExpression[E]): SelectQueryPart[E, E] = {
    SelectQueryPart(SingleExpression(e1), singleValueConverter[E], distinct = false)
  }
  def select[E1, E2](e1: SelectExpression[E1], e2: SelectExpression[E2]): SelectQueryPart[(E1, E2), (E1, E2)] = {
    SelectQueryPart(TwoExpressions(e1, e2), tuple2Converter[E1, E2], distinct = false)
  }
  def select[E1, E2, E3](e1: SelectExpression[E1],
                         e2: SelectExpression[E2],
                         e3: SelectExpression[E3]): SelectQueryPart[(E1, E2, E3), (E1, E2, E3)] = {
    SelectQueryPart(ThreeExpressions(e1, e2, e3), tuple3Converter[E1, E2, E3], distinct = false)
  }
  def select[E1, E2, E3, E4](e1: SelectExpression[E1],
                             e2: SelectExpression[E2],
                             e3: SelectExpression[E3],
                             e4: SelectExpression[E4]): SelectQueryPart[(E1, E2, E3, E4), (E1, E2, E3, E4)] = {
    SelectQueryPart(FourExpressions(e1, e2, e3, e4), tuple4Converter[E1, E2, E3, E4], distinct = false)
  }
  def select[E1, E2, E3, E4, E5](e1: SelectExpression[E1],
                                 e2: SelectExpression[E2],
                                 e3: SelectExpression[E3],
                                 e4: SelectExpression[E4],
                                 e5: SelectExpression[E5]
                                ): SelectQueryPart[(E1, E2, E3, E4, E5), (E1, E2, E3, E4, E5)] = {
    SelectQueryPart(FiveExpressions(e1, e2, e3, e4, e5), tuple5Converter[E1, E2, E3, E4, E5], distinct = false)
  }
  def select[E1, E2, E3, E4, E5, E6](e1: SelectExpression[E1],
                                     e2: SelectExpression[E2],
                                     e3: SelectExpression[E3],
                                     e4: SelectExpression[E4],
                                     e5: SelectExpression[E5],
                                     e6: SelectExpression[E6]
                                    ): SelectQueryPart[(E1, E2, E3, E4, E5, E6), (E1, E2, E3, E4, E5, E6)] = {
    SelectQueryPart(SixExpressions(e1, e2, e3, e4, e5, e6), tuple6Converter[E1, E2, E3, E4, E5, E6], distinct = false)
  }
  def select[E1, E2, E3, E4, E5, E6, E7](e1: SelectExpression[E1],
                                         e2: SelectExpression[E2],
                                         e3: SelectExpression[E3],
                                         e4: SelectExpression[E4],
                                         e5: SelectExpression[E5],
                                         e6: SelectExpression[E6],
                                         e7: SelectExpression[E7]
                                        ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7),
                                                           (E1, E2, E3, E4, E5, E6, E7)] = {
    SelectQueryPart(SevenExpressions(e1, e2, e3, e4, e5, e6, e7), tuple7Converter[E1, E2, E3, E4, E5, E6, E7], distinct = false)
  }
  def select[E1, E2, E3, E4, E5, E6, E7, E8](e1: SelectExpression[E1],
                                             e2: SelectExpression[E2],
                                             e3: SelectExpression[E3],
                                             e4: SelectExpression[E4],
                                             e5: SelectExpression[E5],
                                             e6: SelectExpression[E6],
                                             e7: SelectExpression[E7],
                                             e8: SelectExpression[E8]
                                            ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8),
                                                               (E1, E2, E3, E4, E5, E6, E7, E8)] = {
    SelectQueryPart(EightExpressions(e1, e2, e3, e4, e5, e6, e7, e8), tuple8Converter[E1, E2, E3, E4, E5, E6, E7, E8], distinct = false)
  }
  def select[E1, E2, E3, E4, E5, E6, E7, E8, E9](e1: SelectExpression[E1],
                                                 e2: SelectExpression[E2],
                                                 e3: SelectExpression[E3],
                                                 e4: SelectExpression[E4],
                                                 e5: SelectExpression[E5],
                                                 e6: SelectExpression[E6],
                                                 e7: SelectExpression[E7],
                                                 e8: SelectExpression[E8],
                                                 e9: SelectExpression[E9]
                                                ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8, E9),
                                                                   (E1, E2, E3, E4, E5, E6, E7, E8, E9)] = {
    SelectQueryPart(NineExpressions(e1, e2, e3, e4, e5, e6, e7, e8, e9), tuple9Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9], distinct = false)
  }
  def select[E1, E2, E3, E4, E5, E6, E7, E8, E9, E10](e1: SelectExpression[E1],
                                                      e2: SelectExpression[E2],
                                                      e3: SelectExpression[E3],
                                                      e4: SelectExpression[E4],
                                                      e5: SelectExpression[E5],
                                                      e6: SelectExpression[E6],
                                                      e7: SelectExpression[E7],
                                                      e8: SelectExpression[E8],
                                                      e9: SelectExpression[E9],
                                                      e10: SelectExpression[E10]
                                                     ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8, E9, E10),
                                                                        (E1, E2, E3, E4, E5, E6, E7, E8, E9, E10)] = {
    SelectQueryPart(TenExpressions(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), tuple10Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9, E10], distinct = false)
  }

  def selectDistinct[E](e1: SelectExpression[E]): SelectQueryPart[E, E] = {
    SelectQueryPart(SingleExpression(e1), singleValueConverter[E], distinct = true)
  }
  def selectDistinct[E1, E2](e1: SelectExpression[E1], e2: SelectExpression[E2]): SelectQueryPart[(E1, E2), (E1, E2)] = {
    SelectQueryPart(TwoExpressions(e1, e2), tuple2Converter[E1, E2], distinct = true)
  }
  def selectDistinct[E1, E2, E3](e1: SelectExpression[E1],
                         e2: SelectExpression[E2],
                         e3: SelectExpression[E3]): SelectQueryPart[(E1, E2, E3), (E1, E2, E3)] = {
    SelectQueryPart(ThreeExpressions(e1, e2, e3), tuple3Converter[E1, E2, E3], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4](e1: SelectExpression[E1],
                             e2: SelectExpression[E2],
                             e3: SelectExpression[E3],
                             e4: SelectExpression[E4]): SelectQueryPart[(E1, E2, E3, E4), (E1, E2, E3, E4)] = {
    SelectQueryPart(FourExpressions(e1, e2, e3, e4), tuple4Converter[E1, E2, E3, E4], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5](e1: SelectExpression[E1],
                                 e2: SelectExpression[E2],
                                 e3: SelectExpression[E3],
                                 e4: SelectExpression[E4],
                                 e5: SelectExpression[E5]
                                ): SelectQueryPart[(E1, E2, E3, E4, E5), (E1, E2, E3, E4, E5)] = {
    SelectQueryPart(FiveExpressions(e1, e2, e3, e4, e5), tuple5Converter[E1, E2, E3, E4, E5], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5, E6](e1: SelectExpression[E1],
                                     e2: SelectExpression[E2],
                                     e3: SelectExpression[E3],
                                     e4: SelectExpression[E4],
                                     e5: SelectExpression[E5],
                                     e6: SelectExpression[E6]
                                    ): SelectQueryPart[(E1, E2, E3, E4, E5, E6), (E1, E2, E3, E4, E5, E6)] = {
    SelectQueryPart(SixExpressions(e1, e2, e3, e4, e5, e6), tuple6Converter[E1, E2, E3, E4, E5, E6], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5, E6, E7](e1: SelectExpression[E1],
                                         e2: SelectExpression[E2],
                                         e3: SelectExpression[E3],
                                         e4: SelectExpression[E4],
                                         e5: SelectExpression[E5],
                                         e6: SelectExpression[E6],
                                         e7: SelectExpression[E7]
                                        ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7),
    (E1, E2, E3, E4, E5, E6, E7)] = {
    SelectQueryPart(SevenExpressions(e1, e2, e3, e4, e5, e6, e7), tuple7Converter[E1, E2, E3, E4, E5, E6, E7], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5, E6, E7, E8](e1: SelectExpression[E1],
                                             e2: SelectExpression[E2],
                                             e3: SelectExpression[E3],
                                             e4: SelectExpression[E4],
                                             e5: SelectExpression[E5],
                                             e6: SelectExpression[E6],
                                             e7: SelectExpression[E7],
                                             e8: SelectExpression[E8]
                                            ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8),
    (E1, E2, E3, E4, E5, E6, E7, E8)] = {
    SelectQueryPart(EightExpressions(e1, e2, e3, e4, e5, e6, e7, e8), tuple8Converter[E1, E2, E3, E4, E5, E6, E7, E8], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5, E6, E7, E8, E9](e1: SelectExpression[E1],
                                                 e2: SelectExpression[E2],
                                                 e3: SelectExpression[E3],
                                                 e4: SelectExpression[E4],
                                                 e5: SelectExpression[E5],
                                                 e6: SelectExpression[E6],
                                                 e7: SelectExpression[E7],
                                                 e8: SelectExpression[E8],
                                                 e9: SelectExpression[E9]
                                                ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8, E9),
    (E1, E2, E3, E4, E5, E6, E7, E8, E9)] = {
    SelectQueryPart(NineExpressions(e1, e2, e3, e4, e5, e6, e7, e8, e9), tuple9Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9], distinct = true)
  }
  def selectDistinct[E1, E2, E3, E4, E5, E6, E7, E8, E9, E10](e1: SelectExpression[E1],
                                                      e2: SelectExpression[E2],
                                                      e3: SelectExpression[E3],
                                                      e4: SelectExpression[E4],
                                                      e5: SelectExpression[E5],
                                                      e6: SelectExpression[E6],
                                                      e7: SelectExpression[E7],
                                                      e8: SelectExpression[E8],
                                                      e9: SelectExpression[E9],
                                                      e10: SelectExpression[E10]
                                                     ): SelectQueryPart[(E1, E2, E3, E4, E5, E6, E7, E8, E9, E10),
    (E1, E2, E3, E4, E5, E6, E7, E8, E9, E10)] = {
    SelectQueryPart(TenExpressions(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), tuple10Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9, E10], distinct = true)
  }

  def select(expressions: List[SelectExpression[_]]): SelectQueryPart[scala.Vector[SelectExpression[_]], QueryResult] = {
    SelectQueryPart[Vector[SelectExpression[_]], QueryResult](VariableExpressions(expressions.toVector), DefaultConverter, distinct = false)
  }
  def selectDistinct(expressions: List[SelectExpression[_]]): SelectQueryPart[scala.Vector[SelectExpression[_]], QueryResult] = {
    SelectQueryPart[Vector[SelectExpression[_]], QueryResult](VariableExpressions(expressions.toVector), DefaultConverter, distinct = true)
  }
  def insert(values: ColumnValue[_, _]*): InsertSingle[Int] = {
    InsertSingle[Int](values.head.column.table, values, identity[Int])
  }
  def insertInto(table: Table, values: Any*): InsertSingle[Int] = insert(values.zip(table.columns).map {
    case (value, column) => column.asInstanceOf[Column[Any, Any]](value)
  }: _*)
  def insertBatch(rows: Seq[Seq[ColumnValue[_, _]]]): InsertMultiple = InsertMultiple(rows.head.head.column.table, rows)
  def merge(key: Column[_, _], values: ColumnValue[_, _]*): Merge = Merge(values.head.column.table, key, values.toList)
  def update(values: ColumnValue[_, _]*): Update[Int] = {
    Update[Int](values.head.column.table, values.toList, mapResult = identity[Int])
  }
  def delete(table: Table): Delete = Delete(table)

  implicit def insert2Rows(inserts: Seq[Insert[_]]): Seq[Seq[ColumnValue[_, _]]] = inserts.flatMap(_.rows)
}

object DSLSupport extends DSLSupport {
  val DefaultConverter = (qr: QueryResult) => qr

  def singleValueConverter[E]: QueryResult => E = (qr: QueryResult) => qr.values.head.value.asInstanceOf[E]

  def tuple2Converter[E1, E2]: QueryResult => (E1, E2) = {
    (qr: QueryResult) => qr.values.head.value.asInstanceOf[E1] -> qr.values(1).value.asInstanceOf[E2]
  }

  def tuple3Converter[E1, E2, E3]: QueryResult => (E1, E2, E3) = {
    (qr: QueryResult) => (qr.values.head.value.asInstanceOf[E1], qr.values(1).value.asInstanceOf[E2], qr.values(2).value.asInstanceOf[E3])
  }

  def tuple4Converter[E1, E2, E3, E4]: QueryResult => (E1, E2, E3, E4) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4]
    )
  }

  def tuple5Converter[E1, E2, E3, E4, E5]: QueryResult => (E1, E2, E3, E4, E5) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5]
    )
  }

  def tuple6Converter[E1, E2, E3, E4, E5, E6]: QueryResult => (E1, E2, E3, E4, E5, E6) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5],
      qr.values(5).value.asInstanceOf[E6]
    )
  }

  def tuple7Converter[E1, E2, E3, E4, E5, E6, E7]: QueryResult => (E1, E2, E3, E4, E5, E6, E7) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5],
      qr.values(5).value.asInstanceOf[E6],
      qr.values(6).value.asInstanceOf[E7]
    )
  }

  def tuple8Converter[E1, E2, E3, E4, E5, E6, E7, E8]: QueryResult => (E1, E2, E3, E4, E5, E6, E7, E8) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5],
      qr.values(5).value.asInstanceOf[E6],
      qr.values(6).value.asInstanceOf[E7],
      qr.values(7).value.asInstanceOf[E8]
    )
  }

  def tuple9Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9]: QueryResult => (E1, E2, E3, E4, E5, E6, E7, E8, E9) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5],
      qr.values(5).value.asInstanceOf[E6],
      qr.values(6).value.asInstanceOf[E7],
      qr.values(7).value.asInstanceOf[E8],
      qr.values(8).value.asInstanceOf[E9]
    )
  }

  def tuple10Converter[E1, E2, E3, E4, E5, E6, E7, E8, E9, E10]: QueryResult => (E1, E2, E3, E4, E5, E6, E7, E8, E9, E10) = {
    (qr: QueryResult) => (
      qr.values.head.value.asInstanceOf[E1],
      qr.values(1).value.asInstanceOf[E2],
      qr.values(2).value.asInstanceOf[E3],
      qr.values(3).value.asInstanceOf[E4],
      qr.values(4).value.asInstanceOf[E5],
      qr.values(5).value.asInstanceOf[E6],
      qr.values(6).value.asInstanceOf[E7],
      qr.values(7).value.asInstanceOf[E8],
      qr.values(8).value.asInstanceOf[E9],
      qr.values(9).value.asInstanceOf[E10]
    )
  }
}