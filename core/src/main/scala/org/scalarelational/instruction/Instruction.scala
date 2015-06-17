package org.scalarelational.instruction

import scala.concurrent.Future

import org.scalarelational.model.Datastore

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[R] {
  protected def thisDatastore: Datastore

  def result: R
  final def async: Future[R] = thisDatastore.async(result)
}