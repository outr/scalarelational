package org.scalarelational

import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty

trait Database {
  def table[T <: Table](props: TableProperty*): T = ???     // TODO: implement via Macro
}