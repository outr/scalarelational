package com.outr.query.orm.persistence

import com.outr.query.QueryResult

/**
 * @author Matt Hicks <matt@outr.com>
 */
object DefaultConverter extends Converter {
  def convert2SQL(persistence: Persistence, value: Any) = ConversionResponse(value, None)

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any], query: QueryResult) = {
    val cv = persistence.caseValue
    Some(cv.valueType.convertTo[Any](cv.name, sql))
  }
}
