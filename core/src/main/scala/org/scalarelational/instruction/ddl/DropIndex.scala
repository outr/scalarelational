package org.scalarelational.instruction.ddl


case class DropIndex(indexName: String, ifExists: Boolean = false)