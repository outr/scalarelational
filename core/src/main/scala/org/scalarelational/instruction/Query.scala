package org.scalarelational.instruction

import org.scalarelational._
import org.scalarelational.dsl.DSLSupport
import org.scalarelational.op.Condition
import org.scalarelational.model.Table
import org.scalarelational.result.{QueryResultsIterator, QueryResult}

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
                                      converter: QueryResult[Result] => Result)
                                     (implicit val vectorify: Expressions => Vector[SelectExpression[_]]) extends WhereSupport[Query[Expressions, Result]] {
  lazy val asVector = vectorify(expressions)

  def fields(expressions: SelectExpression[_]*) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = asVector ++ expressions, converter = DSLSupport.DefaultConverter)
  def fields(expressions: Vector[SelectExpression[_]]) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = this.expressions ++ expressions, converter = DSLSupport.DefaultConverter)
  def withoutField(expression: SelectExpression[_]) = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = expressions.filterNot(se => se == expression), converter = DSLSupport.DefaultConverter)
  def clearFields() = copy[Vector[SelectExpression[_]], QueryResult[_]](expressions = Vector.empty, converter = DSLSupport.DefaultConverter)

  def from(table: Table) = copy[Expressions, Result](table = table)
  def where(condition: Condition) = copy[Expressions, Result](whereCondition = condition)

  def join(table: Table, joinType: JoinType = JoinType.Join, alias: String = null) = PartialJoin[Expressions, Result](this, table, joinType, alias)

  def innerJoin(table: Table) = join(table, joinType = JoinType.Inner)
  def innerJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.Inner, alias = alias.tableAlias)

  def leftJoin(table: Table) = join(table, joinType = JoinType.Left)
  def leftJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.Left, alias = alias.tableAlias)

  def leftOuterJoin(table: Table) = join(table, joinType = JoinType.LeftOuter)
  def leftOuterJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.LeftOuter, alias = alias.tableAlias)

  def limit(value: Int) = copy[Expressions, Result](resultLimit = value)
  def offset(value: Int) = copy[Expressions, Result](resultOffset = value)

  def groupBy(expressions: SelectExpression[_]*) = copy[Expressions, Result](grouping = grouping ::: expressions.toList)
  def orderBy(entries: OrderBy[_]*) = copy[Expressions, Result](ordering = entries.toList ::: ordering)

  def convert[NewResult](converter: QueryResult[NewResult] => NewResult) = copy[Expressions, NewResult](converter = converter)
  def map[NewResult](converter: Result => NewResult) = {
    copy[Expressions, NewResult](converter = (qr: QueryResult[NewResult]) => converter(this.converter(qr.asInstanceOf[QueryResult[Result]])))
  }

  def result = new QueryResultsIterator(table.datastore.exec(this), this)
  def async = table.datastore.async {
    result
  }
}