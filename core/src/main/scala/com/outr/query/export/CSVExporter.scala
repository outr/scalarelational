package com.outr.query.export

import com.outr.query.{Datastore, Table}
import java.io.{FileWriter, File}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object CSVExporter {
  private val NewLine = "\r\n"

  def exportDatastore(datastore: Datastore, directory: File) = {
    directory.mkdirs()
    datastore.tables.foreach {
      case table => exportTable(table, new File(directory, s"${table.tableName.toLowerCase}.csv"))
    }
  }

  def exportTable(table: Table, file: File) = {
    val writer = new FileWriter(file)
    try {
      val columnNames = table.columns.map(c => c.name).mkString(",")
      writer.write(columnNames)
      writer.write(NewLine)

      val query = table.datastore.select(table.*) from table orderBy table.primaryKeys.head.asc
      val results = table.datastore.exec(query)
      results.foreach {
        case r => {
          val values = r.values.map {
            case ev => ev.value match {
              case null => "null"
              case v => v.toString
            }
          }
          writer.write(values.mkString(","))
          writer.write(NewLine)
        }
      }
    } finally {
      writer.flush()
      writer.close()
    }
  }
}
