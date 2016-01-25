package org.scalarelational.instruction

import org.scalarelational._
import org.scalarelational.column.{ColumnAlias, ColumnLike}
import org.scalarelational.instruction.query.{JoinSupport, SelectExpressions}
import org.scalarelational.op.Condition
import org.scalarelational.result.{EnhancedIterator, QueryResult, QueryResultsIterator}
import org.scalarelational.table.Table

import scala.concurrent.Future
import scala.language.existentials

case class Query[Types, Result](expressions: SelectExpressions[Types],
                                table: Table,
                                joins: List[Join] = Nil,
                                whereCondition: Option[Condition] = None,
                                grouping: List[SelectExpression[_]] = Nil,
                                ordering: List[OrderBy[_]] = Nil,
                                resultLimit: Int = -1,
                                resultOffset: Int = -1,
                                converter: QueryResult => Result,
                                alias: Option[String] = None) extends WhereSupport[Query[Types, Result]]
                                                              with Joinable
                                                              with JoinSupport[Types, Result] {
  def apply[T, S](column: ColumnLike[T, S]): ColumnAlias[T, S] = ColumnAlias[T, S](column, alias, None, None)

  def where(condition: Condition): Query[Types, Result] = copy[Types, Result](whereCondition = Option(condition))

  def limit(value: Int): Query[Types, Result] = copy[Types, Result](resultLimit = value)
  def offset(value: Int): Query[Types, Result] = copy[Types, Result](resultOffset = value)

  def groupBy(expressions: SelectExpression[_]*): Query[Types, Result] = copy[Types, Result](grouping = grouping ::: expressions.toList)
  def orderBy(entries: OrderBy[_]*): Query[Types, Result] = copy[Types, Result](ordering = entries.toList ::: ordering)

  def as(alias: String): Query[Types, Result] = copy(alias = Option(alias))

  def map[NewResult](converter: Result => NewResult): Query[Types, NewResult] = copy[Types, NewResult](converter = this.converter.andThen(converter))
  def convert[NewResult](converter: QueryResult => NewResult): Query[Types, NewResult] = copy[Types, NewResult](converter = converter)

  def result(implicit session: Session): QueryResultsIterator[Types, Result] = new QueryResultsIterator(table.database.exec(this), this)
  def converted(implicit session: Session): EnhancedIterator[Result] = new EnhancedIterator[Result](result.map(qr => converter(qr)))
  def async: Future[QueryResultsIterator[Types, Result]] = table.database.async { implicit session =>
    result
  }
}

trait ResultConverter[Result] extends (QueryResult => Result)