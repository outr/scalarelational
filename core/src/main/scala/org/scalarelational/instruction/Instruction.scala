package org.scalarelational.instruction

import scala.concurrent.Future

import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[R] {
  def table: Table

  def result: R
  final def async: Future[R] = table.datastore.async(result)

  /**
   * Convenience wrapper that simply calls <code>result</code>
   */
  def apply(): R = result
}