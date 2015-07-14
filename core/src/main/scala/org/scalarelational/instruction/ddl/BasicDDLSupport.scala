package org.scalarelational.instruction.ddl

import org.scalarelational.CallableInstruction
import org.scalarelational.column.property._
import org.scalarelational.model.table.property.Index
import org.scalarelational.model.{Datastore, Column, Table, ColumnPropertyContainer}

import scala.collection.mutable.ListBuffer

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait BasicDDLSupport extends DDLSupport with Datastore {
  override def table2Create(table: Table, ifNotExists: Boolean = true) = {
    val createColumns = table.columns.map(c => column2Create(c))
    CreateTable(table.tableName, ifNotExists = ifNotExists, columns = createColumns, table.properties)
  }
  override def column2Create[T](column: Column[T]): CreateColumn[T] = {
    CreateColumn[T](column.table.tableName, column.name, column.dataType, column.properties)(column.manifest)
  }

  override def ddl(tables: List[Table], ifNotExists: Boolean = true): List[CallableInstruction] = {
    val b = ListBuffer.empty[CallableInstruction]

    tables.foreach {
      case table => {
        // Create Table
        val createTable = table2Create(table)
        b ++= ddl(createTable)
      }
    }

    tables.foreach {
      case table => {
        // Create Table References
        table.foreignKeys.foreach {
          case column => {
            val foreignKey = ForeignKey(column).foreignColumn
            val createForeignKey = CreateForeignKey(table.tableName, column.name, foreignKey.table.tableName, foreignKey.name)
            b ++= ddl(createForeignKey)
          }
        }

        // Create Column Indexes
        table.columns.foreach {
          case column => column.get[Indexed](Indexed.name) match {
            case Some(index) => {
              b ++= ddl(CreateIndex(table.tableName, index.indexName, List(column.name)))
            }
            case None => // No index of this column
          }
        }

        // Create Table Indexes
        table.properties.values.foreach {
          case index: Index => {
            b ++= ddl(CreateIndex(table.tableName, index.indexName, index.columns.map(_.name), index.unique, ifNotExists))
          }
          case _ => // Ignore other table properties
        }
      }
    }

    b.toList
  }

  override def ddl(create: CreateTable): List[CallableInstruction] = {
    val b = new StringBuilder

    b.append("CREATE TABLE ")
    if (create.ifNotExists) {
      b.append("IF NOT EXISTS ")
    }
    b.append(create.name)
    b.append('(')
    b.append(create.columns.map(columnSQL).mkString(", "))

    val primaryKeys = create.columns.collect {
      case cc if cc.has(PrimaryKey) => cc
    }
    if (primaryKeys.nonEmpty) {
      b.append(s", PRIMARY KEY(${primaryKeys.map(c => c.name).mkString(", ")})")
    }

    b.append(");")

    List(CallableInstruction(b.toString()))
  }

  override def ddl[T](create: CreateColumn[T]): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append(s"ALTER TABLE ${create.tableName} ADD ${columnSQL(create)};")
    List(CallableInstruction(b.toString()))
  }

  override def ddl(alter: CreateForeignKey): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append(s"ALTER TABLE ${alter.tableName}\r\n")
    b.append(s"  ADD FOREIGN KEY(${alter.columnName})\r\n")
    b.append(s"  REFERENCES ${alter.foreignTableName}(${alter.foreignColumnName});")
    List(CallableInstruction(b.toString()))
  }

  override def ddl(create: CreateIndex): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append(s"CREATE ")
    if (create.unique) {
      b.append("UNIQUE ")
    }
    b.append("INDEX ")
    if (create.ifNotExists) {
      b.append("IF NOT EXISTS ")
    }
    b.append(create.name)
    b.append(" ON ")
    b.append(create.tableName)
    b.append("(")
    b.append(create.columns.mkString(", "))
    b.append(");")
    List(CallableInstruction(b.toString()))
  }

  override def ddl(drop: DropTable): List[CallableInstruction] = {
    val sql = s"DROP TABLE ${drop.tableName}"
    List(CallableInstruction(sql))
  }

  protected def columnSQL(create: CreateColumn[_]) = {
    val b = new StringBuilder
    b.append(create.name)
    b.append(' ')
    b.append(create.dataType.sqlType(this, create))
    val props = columnPropertiesSQL(create)
    if (props.nonEmpty) {
      b.append(props.mkString(" ", " ", ""))
    }
    b.toString()
  }

  protected def columnPropertiesSQL(container: ColumnPropertyContainer): List[String] = {
    val b = ListBuffer.empty[String]
    if (!container.isOptional && !container.has(Polymorphic)) {
      b.append("NOT NULL")
    }
    if (container.has(AutoIncrement)) {
      b.append("AUTO_INCREMENT")
    }
    if (container.has(Unique)) {
      b.append("UNIQUE")
    }
    b.toList
  }
}