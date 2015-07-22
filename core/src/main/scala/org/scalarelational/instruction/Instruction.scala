package org.scalarelational.instruction

import scala.concurrent.Future

import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[R] {
  protected final def thisDatastore = table.datastore
  def table: Table

  def result: R
  final def async: Future[R] = thisDatastore.async(result)

  /**
   * Convenience wrapper that simply calls <code>result</code>
   */
  def apply(): R = result
}