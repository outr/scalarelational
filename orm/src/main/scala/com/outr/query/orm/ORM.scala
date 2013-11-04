package com.outr.query.orm

import com.outr.query.QueryResult
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
object ORM {
  // TODO: support aliasing of column names to case class fields
  // TODO: support conversion from table types to field types

  def apply[T](result: QueryResult)(implicit manifest: Manifest[T]) = {
    val args = result.values.map(cv => cv.column.name -> cv.value).toMap
    manifest.runtimeClass.copy[T](null.asInstanceOf[T], args)
  }
}
