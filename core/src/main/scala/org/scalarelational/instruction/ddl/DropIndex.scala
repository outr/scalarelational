package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DropIndex(indexName: String, ifExists: Boolean = false)