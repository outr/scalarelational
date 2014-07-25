package com.outr.query

import org.apache.lucene.document._

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object search {
  implicit class ColumnField[T](column: Column[T]) {
    def stringField(value: String, name: String = column.name, store: Boolean = true) = {
      new StringField(name, value, storeField(store))
    }

    def textField(value: String, name: String = column.name, store: Boolean = true) = {
      new TextField(name, value, storeField(store))
    }

    def intField(value: Int, name: String = column.name, store: Boolean = true) = {
      new IntField(name, value, storeField(store))
    }

    def longField(value: Long, name: String = column.name, store: Boolean = true) = {
      new LongField(name, value, storeField(store))
    }

    def floatField(value: Float, name: String = column.name, store: Boolean = true) = {
      new FloatField(name, value, storeField(store))
    }

    def doubleField(value: Double, name: String = column.name, store: Boolean = true) = {
      new DoubleField(name, value, storeField(store))
    }

    def stringFieldOption(value: String, name: String = column.name, store: Boolean = true) = if (value != null && value.trim.nonEmpty) {
      Some(new StringField(name, value, storeField(store)))
    } else {
      None
    }

    def textFieldOption(value: String, name: String = column.name, store: Boolean = true) = if (value != null && value.trim.nonEmpty) {
      Some(new TextField(name, value, storeField(store)))
    } else {
      None
    }
  }

  implicit class TableField(table: Table) {
    def stringField(value: String, name: String, store: Boolean = true) = {
      new StringField(name, value, storeField(store))
    }

    def textField(value: String, name: String, store: Boolean = true) = {
      new TextField(name, value, storeField(store))
    }

    def intField(value: Int, name: String, store: Boolean = true) = {
      new IntField(name, value, storeField(store))
    }

    def longField(value: Long, name: String, store: Boolean = true) = {
      new LongField(name, value, storeField(store))
    }

    def floatField(value: Float, name: String, store: Boolean = true) = {
      new FloatField(name, value, storeField(store))
    }

    def doubleField(value: Double, name: String, store: Boolean = true) = {
      new DoubleField(name, value, storeField(store))
    }

    def stringFieldOption(value: String, name: String, store: Boolean = true) = if (value != null && value.trim.nonEmpty) {
      Some(new StringField(name, value, storeField(store)))
    } else {
      None
    }

    def textFieldOption(value: String, name: String, store: Boolean = true) = if (value != null && value.trim.nonEmpty) {
      Some(new TextField(name, value, storeField(store)))
    } else {
      None
    }
  }

  private def storeField(store: Boolean) = if (store) Field.Store.YES else Field.Store.NO
}