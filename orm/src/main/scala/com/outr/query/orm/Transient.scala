package com.outr.query.orm

/**
 * Transient works similarly to Lazy in that it dynamically loads foreign key references on-demand, but does not cache
 * the request so each call to "use" represents a query to the database. Further, the "use" encapsulates the
 * functionality making this an ideal use-case for things like Blobs that require the connection to remain open while
 * accessing the requested value.
 *
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Transient[T] {
  def manifest: Manifest[T]

  /**
   * Takes a function that will receive the foreign value.
   *
   * @param f the function to receive the value
   * @tparam R the return type
   * @return R
   */
  def use[R](f: Option[T] => R): R

  override def toString = s"${getClass.getSimpleName}(...)"
}

object Transient {
  def None[T](implicit manifest: Manifest[T]) = PreloadedTransient[T](scala.None)
  def apply[T](value: T)(implicit manifest: Manifest[T]) = PreloadedTransient[T](Option(value))
  def fromOption[T](o: Option[T])(implicit manifest: Manifest[T]) = PreloadedTransient[T](o)
}

case class PreloadedTransient[T](value: Option[T])(implicit val manifest: Manifest[T]) extends Transient[T] {
  def use[R](f: Option[T] => R) = f(value)
}

case class DynamicTransient[T](table: ORMTable[T], key: Any)(implicit val manifest: Manifest[T]) extends Transient[T] {
  def use[R](f: Option[T] => R) = table.datastore.transaction {
    val value = table.byId(key)
    f(value)
  }
}