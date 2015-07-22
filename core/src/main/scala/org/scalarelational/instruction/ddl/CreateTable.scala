package org.scalarelational.instruction.ddl

import org.scalarelational.table.TablePropertyContainer
import org.scalarelational.table.property.TableProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateTable(name: String,
                       ifNotExists: Boolean = false,
                       columns: List[CreateColumn[_]] = Nil,
                       props: Seq[TableProperty] = Nil) extends TablePropertyContainer