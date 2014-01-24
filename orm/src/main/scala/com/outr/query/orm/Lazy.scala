package com.outr.query.orm

/**
 * Lazy can be used to lazily load a foreign key on-demand. After the first request to load on the referencing instance
 * all future calls will use the cached value.
 *
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Lazy[T] extends (() => T) {
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