package org.scalarelational.dsl.ddl

class Create private() {
  def table(name: String): CreateTable = CreateTable(name)
}

object Create extends Create