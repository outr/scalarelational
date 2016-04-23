package org.scalarelational.mariadb

import javax.sql.DataSource

import com.mysql.cj.jdbc.MysqlDataSource
import org.scalarelational.Session
import org.scalarelational.datatype.DataType
import org.scalarelational.instruction.ddl.DropTable
import org.scalarelational.instruction.{CallableInstruction, InstructionType, Merge}
import org.scalarelational.model._
import pl.metastack.metarx.Opt

case class MariaDBConfig(host: String,
                         schema: String,
                         user: String,
                         password: String,
                         port: Int = 3306)

abstract class MariaDBDatastore private() extends SQLDatastore {
  protected def this(mariadbConfig: MariaDBConfig) = {
    this()
    config := mariadbConfig
  }

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := dataSource
  }

  /* MariaDB does not support the `MERGE INTO` syntax but overrides the invocation to use INSERT ON DUPLICATE UPDATE.*/
  override def supportsMerge: Boolean = true
  /* This is kind of abitrary, but `DataSource.DefaultVarCharLength` does not
   * work here as it is the row size limit for MariaDB */
  override def DefaultVarCharLength: Int = 200

  Class.forName("com.mysql.jdbc.Driver")

  val config = Opt[MariaDBConfig]()

  // Update the data source if the mode changes
  config.values.attach(updateDataSource)

  override def ddl(drop: DropTable): List[CallableInstruction] =
    if (drop.cascade) {
      List(
        CallableInstruction("SET foreign_key_checks = 0;"),
        super.ddl(drop).head,
        CallableInstruction("SET foreign_key_checks = 1;")
      )
    } else {
      super.ddl(drop)
    }

  def updateDataSource(config: MariaDBConfig): Unit = {
    dispose() // Make sure to shut down the previous DataSource if possible
    val source = new MysqlDataSource()
    source.setURL("jdbc:mysql://" + config.host + "/" + config.schema)
    source.setUser(config.user)
    source.setPassword(config.password)
    source.setPort(config.port)
    dataSourceProperty := source
  }

  override protected def invoke(merge: Merge)(implicit session: Session): Int = {
    val table = merge.table
    val columnNames = merge.values.map(_.column.name).mkString(", ")
    val columnValues = merge.values.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL))
    val placeholder = columnValues.map(v => "?").mkString(", ")

    val sets = merge.values.map(cv => s"${cv.column.name}=?").mkString(", ")
    val setArgs = merge.values.map(cv => cv.column.dataType.asInstanceOf[DataType[Any, Any]].typed(cv.toSQL))
    val args = columnValues ::: setArgs

    val insertString = s"INSERT INTO ${table.tableName} ($columnNames) VALUES ($placeholder) ON DUPLICATE KEY UPDATE $sets"
    SQLContainer.calling(table, InstructionType.Insert, insertString)
    val resultSet = session.executeInsert(insertString, args)
    try {
      if (resultSet.next()) {
        resultSet.getInt(1)  // TODO This restricts the PKs on PostgreSQL to integers
      } else {
        -1
      }
    } finally {
      resultSet.close()
    }
  }
}
