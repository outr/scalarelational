package com.outr.query.orm.persistence

import org.powerscala.reflect.CaseValue
import com.outr.query.{QueryResult, ColumnValue, Column}
import com.outr.query.orm.ORMTable
import scala.language.existentials
import com.outr.query.orm.convert.ORMConverter

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Persistence[T, C, O](table: ORMTable[T],
                                caseValue: CaseValue,
                                column: Column[C],
                                converter: ORMConverter[C, O]) {
  def conversion(instance: T) = {
    val o = caseValue[O](instance.asInstanceOf[AnyRef])
    converter.fromORM(column, o)
  }

  def conversion(columnValue: ColumnValue[C], result: QueryResult) = {
    converter.toORM(column, columnValue.value, result)
  }
}