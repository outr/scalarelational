package org.scalarelational.instruction.ddl

import org.scalarelational.table.TablePropertyContainer
import org.scalarelational.table.property.TableProperty


case class CreateTable(name: String,
                       ifNotExists: Boolean = false,
                       columns: List[CreateColumn[_, _]] = Nil,
                       props: Seq[TableProperty] = Nil) extends TablePropertyContainer