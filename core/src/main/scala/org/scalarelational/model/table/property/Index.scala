package org.scalarelational.model.table.property

import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Index private(val indexName: String,
                    val unique: Boolean,
                    val columns: List[ColumnLike[_]]) extends TableProperty {
  override val name = s"index(${columns.map(c => c.name).mkString(", ")}})"
}

object Index {
  def apply(name: String, columns: ColumnLike[_]*) = new Index(name, false, columns.toList)
  def unique(name: String, columns: ColumnLike[_]*) = new Index(name, true, columns.toList)
}