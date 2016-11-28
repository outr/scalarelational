package org.scalarelational.mariadb

import javax.sql.DataSource

import com.mysql.cj.jdbc.MysqlDataSource
import com.outr.props.Var
import org.scalarelational.Session
import org.scalarelational.datatype.DataType
import org.scalarelational.instruction.ddl.DropTable
import org.scalarelational.instruction.{CallableInstruction, InstructionType, Merge}
import org.scalarelational.model._

case class MariaDBConfig(host: String,
                         schema: String,
                         user: String,
                         password: String,
                         port: Int = 3306,
                         serverTimezone: Option[String] = None)

abstract class MariaDBDatastore private() extends SQLDatastore {
  protected def this(mariadbConfig: MariaDBConfig) = {
    this()
    config := Some(mariadbConfig)
  }

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := Some(dataSource)
  }

  /* MariaDB does not support the `MERGE INTO` syntax but overrides the invocation to use INSERT ON DUPLICATE UPDATE.*/
  override def supportsMerge: Boolean = true

  /**
    * Depending on the character set, the maximum length is either 255 (utf8) or
    * 191 (utf8m4). Therefore, we will override the default limit set by
    * [[Datastore.DefaultVarCharLength]].
    */
  override def DefaultVarCharLength: Int = 191

  Class.forName("com.mysql.jdbc.Driver")

  val config: Var[Option[MariaDBConfig]] = Var(None)

  // Update the data source if the mode changes
  config.attach(updateDataSource)

  override protected def catalog: Option[String] = config.get.map(_.schema)

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

  def updateDataSource(configOption: Option[MariaDBConfig]): Unit = configOption.foreach { config =>
    dispose() // Make sure to shut down the previous DataSource if possible
    val source = new MysqlDataSource()
    source.setURL("jdbc:mysql://" + config.host + "/" + config.schema +
      config.serverTimezone.fold("")("?serverTimezone=" + _))
    source.setUser(config.user)
    source.setPassword(config.password)
    source.setPort(config.port)
    dataSourceProperty := Some(source)
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
