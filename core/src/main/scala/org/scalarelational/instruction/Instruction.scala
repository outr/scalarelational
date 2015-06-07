package org.scalarelational.instruction

import java.util.concurrent.Future

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Instruction[R] {
  def result: R
  def async: Future[R]
}