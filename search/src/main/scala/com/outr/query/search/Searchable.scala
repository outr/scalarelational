package com.outr.query.search

import org.apache.lucene.document.Field
import org.apache.lucene.facet.taxonomy.CategoryPath
import com.outr.query.Column
import com.outr.query.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Searchable[T] extends ColumnProperty {
  val name = "searchable"

  def field(c: Column[T], value: T): Option[Field]

  def categoryPaths: List[CategoryPath]
}

//class StringSearchable extends Searchable[String] {
//  override def field(c: Column[String], value: String) = Some(new StringField())
//}