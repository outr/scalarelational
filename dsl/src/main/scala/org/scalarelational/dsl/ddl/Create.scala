package org.scalarelational.dsl.ddl

class Create private() {
  def table(name: String, ifNotExists: Boolean = false): CreateTable = CreateTable(name, ifNotExists)
}

object Create extends Create