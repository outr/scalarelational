package org.scalarelational

import org.scalarelational.gen.TableGeneration
import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty

import scala.language.experimental.macros

trait Database {
  def table[T <: Table](props: TableProperty*): T = macro TableGeneration.create[T]
}