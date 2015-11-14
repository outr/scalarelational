package org.scalarelational.mariadb

import javax.sql.DataSource

import org.mariadb.jdbc.MariaDbDataSource
import org.powerscala.log.Logging
import org.powerscala.property.Property
import org.scalarelational.instruction.CallableInstruction
import org.scalarelational.instruction.ddl.DropTable
import org.scalarelational.model._

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class MariaDBConfig(host: String,
                         schema: String,
                         user: String,
                         password: String,
                         port: Int = 3306)

abstract class MariaDBDatastore private() extends SQLDatastore with Logging {
  protected def this(mariadbConfig: MariaDBConfig) = {
    this()
    config := mariadbConfig
  }

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := dataSource
  }

  /* MariaDB does not support the `MERGE INTO` syntax.*/
  override def supportsMerge = false
  /* This is kind of abitrary, but `DataSource.DefaultVarCharLength` does not
   * work here as it is the row size limit for MariaDB */
  override def DefaultVarCharLength = 200

  Class.forName("org.mariadb.jdbc.Driver")

  val config = Property[MariaDBConfig]()

  init()

  protected def init() = {
    config.change.on {
      case evt => updateDataSource() // Update the data source if the mode changes
    }
  }

  override def ddl(drop: DropTable): List[CallableInstruction] =
    if (drop.cascade) List(CallableInstruction("SET foreign_key_checks = 0;"),
                           super.ddl(drop).head,
                           CallableInstruction("SET foreign_key_checks = 1;"))
    else super.ddl(drop)

  def updateDataSource() = {
    dispose() // Make sure to shut down the previous DataSource if possible
    val source: MariaDbDataSource = new MariaDbDataSource()
    source.setUrl("jdbc:mariadb://" + config().host + "/" + config().schema)
    source.setUser(config().user)
    source.setPassword(config().password)
    source.setPort(config().port)
    dataSourceProperty := source
  }
}
