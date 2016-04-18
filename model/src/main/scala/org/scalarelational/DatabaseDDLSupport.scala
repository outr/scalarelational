package org.scalarelational

import java.sql.Timestamp

import org.scalarelational.dsl.ddl.{CreateColumn, CreateColumnEntry, CreateTable, CreateTableAndColumns}
import org.scalarelational.instruction.Instruction
import org.scalarelational.table.Table
import org.scalarelational.table.property.TableName

class DatabaseDDLSupport[D <: Database] private(database: D) {
  /**
    * Create an Instruction representation to create the supplied tables. At least one table must be provided.
    *
    * @param tables the tables to create or empty to create all
    * @return Instruction
    */
  def createTables(tables: Table*): Instruction[D, Boolean] = {
    if (tables.isEmpty) throw new RuntimeException(s"createTables must be called with at least one table.")
    val instructions = tables.map { table =>
//      println(s"Table Name: ${table.prop(TableName)}")
//      val tableName = table.prop(TableName).get.name
//      val columns = table.columns.map { column =>
//        new CreateColumn[]()
//      }
//      database.create.table(tableName)(columns: _*)
    }
    // TODO: finish
    ???
  }

  object create {
    def table(name: String, ifNotExists: Boolean = false)(columns: CreateColumnEntry[_]*): CreateTableAndColumns[D] = {
      CreateTable(database, name, ifNotExists)(columns: _*)
    }

    def int(name: String): CreateColumnEntry[Int] = CreateColumnEntry(name, "INTEGER")

    def varchar(name: String, length: Int = 255): CreateColumnEntry[String] = CreateColumnEntry(name, s"VARCHAR($length)")

    def timestamp(name: String): CreateColumnEntry[Timestamp] = CreateColumnEntry(name, "TIMESTAMP")
  }
}

object DatabaseDDLSupport {
  private var map = Map.empty[Database, DatabaseDDLSupport[_]]

  def apply[D <: Database](database: D): DatabaseDDLSupport[D] = synchronized {
    map.get(database) match {
      case Some(dds) => dds.asInstanceOf[DatabaseDDLSupport[D]]
      case None => {
        val dds = new DatabaseDDLSupport[D](database)
        map += database -> dds
        dds
      }
    }
  }
}