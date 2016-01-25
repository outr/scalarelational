package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.table.Table

import scala.concurrent.Future


trait Instruction[+R] {
  def table: Table

  def result(implicit session: Session): R
  final def async: Future[R] = table.database.async { implicit session =>
    result
  }

  /**
   * Convenience wrapper that simply calls <code>result</code>
   */
  def apply()(implicit session: Session): R = result
}