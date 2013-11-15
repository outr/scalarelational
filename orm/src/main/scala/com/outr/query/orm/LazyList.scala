package com.outr.query.orm

import com.outr.query.Query
import org.powerscala.Priority
import com.outr.query.orm.persistence.LazyListConverter
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait LazyList[T] extends (() => List[T]) {
  LazyList

  lazy val clazz: EnhancedClass = manifest.runtimeClass
  if (clazz.javaClass == classOf[AnyRef]) {
    throw new RuntimeException("Cannot set LazyList generic type to AnyRef.")
  }

  def manifest: Manifest[T]

  def loaded: Boolean
}

object LazyList {
  ORMTable.persistenceSupport.listen(Priority.Low) {    // Support lazy list converter
    case persistence => if (persistence.column == null && persistence.caseValue.valueType.hasType(classOf[LazyList[_]])) {
      persistence.copy(converter = new LazyListConverter(persistence.table.asInstanceOf[ORMTable[Any]], persistence.caseValue))
    } else {
      persistence
    }
  }

  def apply[T](values: T*)(implicit manifest: Manifest[T]) = PreloadedLazyList[T](values.toList)

  def Empty[T](implicit manifest: Manifest[T]) = PreloadedLazyList[T](Nil)
}

case class PreloadedLazyList[T](values: List[T])(implicit val manifest: Manifest[T]) extends LazyList[T] {
  def loaded = true

  def apply() = values
}

case class DelayedLazyList[T](table: ORMTable[T], query: Query)(implicit val manifest: Manifest[T]) extends LazyList[T] {
  @volatile private var _loaded = false
  @volatile private var values: List[T] = null
  def loaded = _loaded

  def apply() = synchronized {
    if (!loaded) {
      load()
    }
    values
  }

  private def load() = {
//    val query = Query(table.*, table).where(conditions)
    values = table.query(query).toList
    _loaded = true
  }
}