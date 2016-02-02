package org.scalarelational

import javax.sql.DataSource

import org.scalarelational.gen.TableGeneration
import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty

import scala.language.experimental.macros

trait Database {
  def dataSource: DataSource
  def tables: Vector[Table] = macro TableGeneration.tables

  private[scalarelational] def namesMap: Map[Table, String] = macro TableGeneration.tablesMap

  def table[T <: Table](props: TableProperty*): T = macro TableGeneration.create[T]
}