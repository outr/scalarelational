package org.scalarelational.instruction

import java.sql.ResultSet

import org.scalarelational._
import org.scalarelational.op.Condition
import org.scalarelational.model.Table
import org.scalarelational.result.{QueryResultsIterator, QueryResult}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Query[Expressions, R] extends WhereSupport[Query[Expressions, R]] {
  def expressions: Expressions
  def table: Table
  def joins: List[Join]
  def whereCondition: Condition
  def grouping: List[SelectExpression[_]]
  def ordering: List[OrderBy[_]]
  def resultLimit: Int
  def resultOffset: Int
  def converter: QueryResult => R

  def expressionsList: List[SelectExpression[_]]

  def from(table: Table) = copy[R](table = table)
  def where(condition: Condition) = copy[R](whereCondition = condition)

  def join(table: Table, joinType: JoinType = JoinType.Join, alias: String = null) = PartialJoin[Expressions, R](this, table, joinType, alias)

  def innerJoin(table: Table) = join(table, joinType = JoinType.Inner)
  def innerJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.Inner, alias = alias.tableAlias)

  def leftJoin(table: Table) = join(table, joinType = JoinType.Left)
  def leftJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.Left, alias = alias.tableAlias)

  def leftOuterJoin(table: Table) = join(table, joinType = JoinType.LeftOuter)
  def leftOuterJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.LeftOuter, alias = alias.tableAlias)

  def limit(value: Int) = copy[R](resultLimit = value)
  def offset(value: Int) = copy[R](resultOffset = value)

  def groupBy(expressions: SelectExpression[_]*) = copy[R](grouping = grouping ::: expressions.toList)
  def orderBy(entries: OrderBy[_]*) = copy[R](ordering = entries.toList ::: ordering)

  def mapped[Result](converter: QueryResult => Result) = copy[Result](converter = converter)

  def result = new QueryResultsIterator(table.datastore.exec(this), this)
  def async = table.datastore.async {
    result
  }

  def copy[Result](expressions: Expressions = expressions,
                   table: Table = table,
                   joins: List[Join] = joins,
                   whereCondition: Condition = whereCondition,
                   grouping: List[SelectExpression[_]] = grouping,
                   ordering: List[OrderBy[_]] = ordering,
                   resultLimit: Int = resultLimit,
                   resultOffset: Int = resultOffset,
                   converter: QueryResult => Result = converter): Query[Expressions, Result]
}

class BasicQuery[R](val expressions: List[SelectExpression[_]],
                    val table: Table = null,
                    val joins: List[Join] = Nil,
                    val whereCondition: Condition = null,
                    val grouping: List[SelectExpression[_]] = Nil,
                    val ordering: List[OrderBy[_]] = Nil,
                    val resultLimit: Int = -1,
                    val resultOffset: Int = -1,
                    val converter: QueryResult => R) extends Query[List[SelectExpression[_]], R] {
  def expressionsList = expressions

  def fields(expressions: SelectExpression[_]*) = copy(expressions = this.expressions ::: expressions.toList)
  def fields(expressions: List[SelectExpression[_]]) = copy(expressions = this.expressions ::: expressions)
  def withoutField(expression: SelectExpression[_]) = copy(expressions = expressions.filterNot(se => se == expression))
  def clearFields() = copy(expressions = Nil)

  override def copy[Result](expressions: List[SelectExpression[_]] = expressions,
                   table: Table = table,
                   joins: List[Join] = joins,
                   whereCondition: Condition = whereCondition,
                   grouping: List[SelectExpression[_]] = grouping,
                   ordering: List[OrderBy[_]] = ordering,
                   resultLimit: Int = resultLimit,
                   resultOffset: Int = resultOffset,
                   converter: QueryResult => Result = converter) = {
    new BasicQuery[Result](expressions, table, joins, whereCondition, grouping, ordering, resultLimit, resultOffset, converter)
  }
}


class SingleExpressionsQuery[R, E](val expressions: SelectExpression[E],
                                val table: Table = null,
                                val joins: List[Join] = Nil,
                                val whereCondition: Condition = null,
                                val grouping: List[SelectExpression[_]] = Nil,
                                val ordering: List[OrderBy[_]] = Nil,
                                val resultLimit: Int = -1,
                                val resultOffset: Int = -1,
                                val converter: QueryResult => R) extends Query[SelectExpression[E], R] {
  def expressionsList = List(expressions)

  def copy[Result](expressions: SelectExpression[E] = expressions,
                   table: Table = table,
                   joins: List[Join] = joins,
                   whereCondition: Condition = whereCondition,
                   grouping: List[SelectExpression[_]] = grouping,
                   ordering: List[OrderBy[_]] = ordering,
                   resultLimit: Int = resultLimit,
                   resultOffset: Int = resultOffset,
                   converter: QueryResult => Result = converter) = {
    new SingleExpressionsQuery[Result, E](expressions, table, joins, whereCondition, grouping, ordering, resultLimit, resultOffset, converter)
  }

  override def result = new SingleExpressionQueryResultsIterator(table.datastore.exec(this), this)
  override def async = table.datastore.async {
    result
  }
}

class SingleExpressionQueryResultsIterator[R, E](rs: ResultSet, query: Query[SelectExpression[E], R]) extends QueryResultsIterator(rs, query) {
  def value = valueFromExpressions(query.expressions, 1)
}
/*
class Tuple2Query[R, E1, E2](val expressions: (E1, E2),
                             val table: Table = null,
                             val joins: List[Join] = Nil,
                             val whereCondition: Condition = null,
                             val grouping: List[SelectExpression] = Nil,
                             val ordering: List[OrderBy] = Nil,
                             val resultLimit: Int = -1,
                             val resultOffset: Int = -1,
                             val converter: QueryResult => R) extends Query[(E1, E2), R] {
  def expressionsList = List(expressions._1, expressions._2)

  def copy[Result](expressions: SelectExpression = expressions,
                   table: Table = table,
                   joins: List[Join] = joins,
                   whereCondition: Condition = whereCondition,
                   grouping: List[SelectExpression] = grouping,
                   ordering: List[OrderBy] = ordering,
                   resultLimit: Int = resultLimit,
                   resultOffset: Int = resultOffset,
                   converter: QueryResult => Result = converter) = {
    new SingleExpressionsQuery[Result](expressions, table, joins, whereCondition, grouping, ordering, resultLimit, resultOffset, converter)
  }
}*/
