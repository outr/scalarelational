package org.scalarelational.instruction.ddl

import org.scalarelational.table.property.Index

import scala.collection.mutable.ListBuffer

import org.scalarelational.table.Table
import org.scalarelational.column.{ColumnPropertyContainer, Column}
import org.scalarelational.column.property._
import org.scalarelational.model.Datastore
import org.scalarelational.instruction.CallableInstruction

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait BasicDDLSupport extends DDLSupport with Datastore {
  override def table2Create(table: Table[_], ifNotExists: Boolean = true) = {
    val createColumns = table.columns.map(c => column2Create(c))
    CreateTable(table.tableName, ifNotExists = ifNotExists, columns = createColumns, table.properties.values.toSeq)
  }
  override def column2Create[T](column: Column[T]): CreateColumn[T] = {
    CreateColumn[T](column.table.tableName, column.name, column.dataType, column.properties.values.toSeq)(column.manifest)
  }

  override def ddl(tables: List[Table[_]], ifNotExists: Boolean = true): List[CallableInstruction] = {
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

  override def ddl[T](alter: ChangeColumnType[T]): List[CallableInstruction] = {
    val properties = ColumnPropertyContainer[T](alter.properties: _*)(alter.manifest)
    val sql = s"ALTER TABLE ${alter.tableName} ALTER COLUMN ${alter.columnName} ${alter.dataType.sqlType(this, properties)}"
    List(CallableInstruction(sql))
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

  override def ddl(alter: RenameColumn): List[CallableInstruction] = {
    val sql = s"ALTER TABLE ${alter.tableName} ALTER COLUMN ${alter.oldName} RENAME TO ${alter.newName}"
    List(CallableInstruction(sql))
  }

  override def ddl(alter: RestartColumn): List[CallableInstruction] = {
    val sql = s"ALTER TABLE ${alter.tableName} ALTER COLUMN ${alter.columnName} RESTART WITH ${alter.value}"
    List(CallableInstruction(sql))
  }

  override def ddl(drop: DropTable): List[CallableInstruction] = {
    val sql = s"DROP TABLE ${drop.tableName}"
    List(CallableInstruction(sql))
  }

  override def ddl(drop: DropColumn): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append("ALTER TABLE ")
    b.append(drop.tableName)
    b.append(" DROP COLUMN ")
    if (drop.ifExists) {
      b.append("IF EXISTS ")
    }
    b.append(drop.columnName)
    List(CallableInstruction(b.toString()))
  }

  override def ddl(drop: DropIndex): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append("DROP INDEX ")
    if (drop.ifExists) {
      b.append("IF EXISTS ")
    }
    b.append(drop.indexName)
    List(CallableInstruction(b.toString()))
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