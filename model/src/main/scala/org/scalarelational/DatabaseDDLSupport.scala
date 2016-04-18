package org.scalarelational

import org.scalarelational.dsl.Instruction
import org.scalarelational.table.Table

class DatabaseDDLSupport[D <: Database] private(database: D) {
  /**
    * Creates the supplied tables or all tables for the database if no tables are provided.
    *
    * @param tables the tables to create or empty to create all
    * @return Instruction
    */
  def createTables(tables: Table*): Instruction = {
    val seq = if (tables.isEmpty) database.tables else tables
    // TODO: finish
    ???
  }
}

object DatabaseDDLSupport {
  private var map = Map.empty[Database, DatabaseDDLSupport[_]]

  def apply[D <: Database](database: D): DatabaseDDLSupport[D] = synchronized {
    map.get(database) match {
      case Some(dds) => dds.asInstanceOf[DatabaseDDLSupport[D]]
      case None => {
        val dds = DatabaseDDLSupport[D](database)
        map += database -> dds
        dds
      }
    }
  }
}