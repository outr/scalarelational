package org.scalarelational.datatype

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
case class Ref[+T](id: Int) {
  override def toString: String = id.toString
}

object Ref {
  def apply[T](t: Id[T]): Ref[T] = Ref[T](t.id.get)
}

trait Id[T] {
  def id: Option[Int]
  def ref: Ref[T] = Ref[T](id.get)
}