package org.scalarelational.instruction.ddl

import org.scalarelational.model.TablePropertyContainer
import org.scalarelational.model.table.property.TableProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateTable(name: String,
                       ifNotExists: Boolean = false,
                       columns: List[CreateColumn[_]] = Nil,
                       properties: Map[String, TableProperty] = Map.empty) extends TablePropertyContainer