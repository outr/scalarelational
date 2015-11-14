package org.scalarelational.instruction

import org.scalarelational.table.Table

import scala.concurrent.Future

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[+R] {
  def table: Table

  def result: R
  final def async: Future[R] = table.datastore.async(result)

  /**
   * Convenience wrapper that simply calls <code>result</code>
   */
  def apply(): R = result
}