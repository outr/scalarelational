package com.outr.query.orm

import org.powerscala.Priority
import com.outr.query.orm.persistence.LazyConverter

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Lazy[T] extends (() => T) {
  Lazy      // Make sure companion has loaded

  def manifest: Manifest[T]

  def loaded: Boolean
  def get(): Option[T]

  def apply() = get().get
  def getOrElse(f: => T) = get() match {
    case Some(t) => t
    case None => f
  }

  override def toString() = s"${getClass.getSimpleName}(${get()})"
}

object Lazy {
  ORMTable.persistenceSupport.listen(Priority.Low) {    // Support lazy converter
    case persistence => if (persistence.column == null && persistence.caseValue.valueType.hasType(classOf[Lazy[_]])) {
      val name = persistence.caseValue.name
      val column = persistence.table.columnsByName[Any](s"${name}_id", s"${name}id", s"${name}_fk", s"${name}fk").collect {
        case c if c.foreignKey.nonEmpty => c
      }.headOption.getOrElse(throw new RuntimeException(s"Unable to find foreign key column for ${persistence.table.tableName}.${persistence.caseValue.name} (Lazy)"))
      val converter = LazyConverter
      persistence.copy(column = column, converter = converter)
    } else {
      persistence
    }
  }

  def None[T](implicit manifest: Manifest[T]) = PreloadedLazy[T](scala.None)
  def apply[T](value: T)(implicit manifest: Manifest[T]) = PreloadedLazy[T](Option(value))
}

case class PreloadedLazy[T](value: Option[T])(implicit val manifest: Manifest[T]) extends Lazy[T] {
  def loaded = true

  def get() = value
}

case class DelayedLazy[T](table: ORMTable[T], key: Any)(implicit val manifest: Manifest[T]) extends Lazy[T] {
  @volatile private var _loaded = false
  @volatile private var value: Option[T] = null
  def loaded = _loaded

  def get() = synchronized {
    if (!loaded) {
      load()
    }
    value
  }
  
  private def load() = {
    value = table.byId(key)
    _loaded = true
  }
}