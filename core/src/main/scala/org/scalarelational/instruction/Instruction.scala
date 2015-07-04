package org.scalarelational.instruction

import scala.concurrent.Future

import org.scalarelational.model.{Table, Datastore}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[R] {
  protected final def thisDatastore = table.datastore
  def table: Table

  def result: R
  final def async: Future[R] = thisDatastore.async(result)
}