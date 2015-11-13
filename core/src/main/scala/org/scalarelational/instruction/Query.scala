package org.scalarelational.instruction

import org.powerscala.reflect._
import org.scalarelational._
import org.scalarelational.column.{ColumnAlias, ColumnLike}
import org.scalarelational.instruction.query.SelectExpressions
import org.scalarelational.op.Condition
import org.scalarelational.result.{QueryResult, QueryResultsIterator}
import org.scalarelational.table.Table

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query[Types, Result](expressions: SelectExpressions[Types],
                                table: Table = null,
                                joins: List[Join] = Nil,
                                whereCondition: Condition = null,
                                grouping: List[SelectExpression[_]] = Nil,
                                ordering: List[OrderBy[_]] = Nil,
                                resultLimit: Int = -1,
                                resultOffset: Int = -1,
                                converter: QueryResult => Result,
                                alias: Option[String] = None) extends WhereSupport[Query[Types, Result]] with Joinable {
  def apply[T, S](column: ColumnLike[T, S]) = ColumnAlias[T, S](column, alias, None, None)

  def from(table: Table) = copy[Types, Result](table = table)
  def where(condition: Condition) = copy[Types, Result](whereCondition = condition)

  def join(joinable: Joinable, joinType: JoinType = JoinType.Join) = PartialJoin[Types, Result](this, joinable, joinType)
  def innerJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Inner)
  def leftJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Left)
  def leftOuterJoin(joinable: Joinable) = join(joinable, joinType = JoinType.LeftOuter)

  def limit(value: Int) = copy[Types, Result](resultLimit = value)
  def offset(value: Int) = copy[Types, Result](resultOffset = value)

  def groupBy(expressions: SelectExpression[_]*) = copy[Types, Result](grouping = grouping ::: expressions.toList)
  def orderBy(entries: OrderBy[_]*) = copy[Types, Result](ordering = entries.toList ::: ordering)

  def as(alias: String) = copy(alias = Option(alias))

  def map[NewResult](converter: Result => NewResult) = copy[Types, NewResult](converter = this.converter.andThen(converter))
  def convert[NewResult](converter: QueryResult => NewResult) = copy[Types, NewResult](converter = converter)

  def result = new QueryResultsIterator(table.datastore.exec(this), this)
  def converted = result.converted
  def async = table.datastore.async {
    result
  }

  def asCase[R](classForRow: QueryResult => Class[_])(implicit manifest: Manifest[R]): Query[Types, R] = {
    convert[R] { r =>
      val clazz = classForRow(r)
      clazz.create[R](r.toFieldMap)
    }
  }
}

trait ResultConverter[Result] extends (QueryResult => Result)