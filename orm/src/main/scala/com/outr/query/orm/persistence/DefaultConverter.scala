package com.outr.query.orm.persistence

/**
 * @author Matt Hicks <matt@outr.com>
 */
object DefaultConverter extends Converter {
  def convert2SQL(persistence: Persistence, value: Any) = ConversionResponse(value, None)

  def convert2Value(persistence: Persistence, sql: Any) = {
    val cv = persistence.caseValue
    Some(cv.valueType.convertTo[Any](cv.name, sql))
  }
}
