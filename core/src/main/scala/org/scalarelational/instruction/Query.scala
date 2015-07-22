package org.scalarelational.instruction

import org.scalarelational._
import org.scalarelational.dsl.DSLSupport
import org.scalarelational.op.Condition
import org.scalarelational.result.{QueryResultsIterator, QueryResult}
import org.scalarelational.table.Table
import org.scalarelational.column.{ColumnAlias, ColumnLike}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query[Expressions, Result](expressions: Expressions,
                                      table: Table = null,
                                      joins: List[Join] = Nil,
                                      whereCondition: Condition = null,
                                      grouping: List[SelectExpression[_]] = Nil,
                                      ordering: List[OrderBy[_]] = Nil,
                                      resultLimit: Int = -1,
                                      resultOffset: Int = -1,
                                      converter: QueryResult[Result] => Result,
                                      alias: Option[String] = None)
                                     (implicit val vectorify: Expressions => Vector[SelectExpression[_]]) extends WhereSupport[Query[Expressions, Result]] with Joinable {
  lazy val asVector = vectorify(expressions)

  def apply[T](column: ColumnLike[T]) = ColumnAlias[T](column, alias, None, None)

  def fields(expressions: SelectExpression[_]*) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = asVector ++ expressions, converter = DSLSupport.DefaultConverter)
  def fields(expressions: Vector[SelectExpression[_]]) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = this.expressions ++ expressions, converter = DSLSupport.DefaultConverter)
  def withoutField(expression: SelectExpression[_]) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = expressions.filterNot(se => se == expression), converter = DSLSupport.DefaultConverter)
  def clearFields() = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = Vector.empty, converter = DSLSupport.DefaultConverter)

  def from(table: Table) = copy[Expressions, Result](table = table)
  def where(condition: Condition) = copy[Expressions, Result](whereCondition = condition)

  def join(joinable: Joinable, joinType: JoinType = JoinType.Join) = PartialJoin[Expressions, Result](this, joinable, joinType)
  def innerJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Inner)
  def leftJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Left)
  def leftOuterJoin(joinable: Joinable) = join(joinable, joinType = JoinType.LeftOuter)

  def limit(value: Int) = copy[Expressions, Result](resultLimit = value)
  def offset(value: Int) = copy[Expressions, Result](resultOffset = value)

  def groupBy(expressions: SelectExpression[_]*) = copy[Expressions, Result](grouping = grouping ::: expressions.toList)
  def orderBy(entries: OrderBy[_]*) = copy[Expressions, Result](ordering = entries.toList ::: ordering)

  def as(alias: String) = copy(alias = Option(alias))

  def convert[NewResult](converter: QueryResult[NewResult] => NewResult) = copy[Expressions, NewResult](converter = converter)
  def map[NewResult](converter: Result => NewResult) = {
    copy[Expressions, NewResult](converter = (qr: QueryResult[NewResult]) => converter(this.converter(qr.asInstanceOf[QueryResult[Result]])))
  }

  def result = new QueryResultsIterator(table.datastore.exec(this), this)
  def async = table.datastore.async {
    result
  }
}