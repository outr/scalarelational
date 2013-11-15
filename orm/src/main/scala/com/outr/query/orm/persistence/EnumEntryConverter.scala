package com.outr.query.orm.persistence

import com.outr.query.QueryResult
import org.powerscala.reflect.EnhancedClass
import org.powerscala.enum.{EnumEntry, Enumerated}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class EnumEntryConverter(enumEntryClass: EnhancedClass) extends Converter {
  val enumerated = enumEntryClass.instance.getOrElse(throw new RuntimeException(s"Unable to find companion for: $enumEntryClass")).asInstanceOf[Enumerated[_]]

  def convert2SQL(persistence: Persistence, value: Any) = {
    val enumName = value match {
      case null => null
      case e: EnumEntry => e.name
    }
    ConversionResponse(enumName, None)
  }

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any], result: QueryResult) = sql match {
    case null => None
    case s: String => Some(enumerated(s))
  }
}
