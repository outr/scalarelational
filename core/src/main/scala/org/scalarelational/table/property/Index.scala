package org.scalarelational.table.property

import org.scalarelational.column.Column


class Index private(val indexName: String,
                    val unique: Boolean,
                    val columns: List[Column[_, _]]) extends TableProperty {
  override val name = s"index(${columns.map(c => c.name).mkString(", ")}})"
}

object Index {
  def apply(name: String, columns: Column[_, _]*): Index = new Index(name, false, columns.toList)
  def unique(name: String, columns: Column[_, _]*): Index = new Index(name, true, columns.toList)
}