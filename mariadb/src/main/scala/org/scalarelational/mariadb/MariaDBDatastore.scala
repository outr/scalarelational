package org.scalarelational.mariadb

import javax.sql.DataSource

import com.mysql.jdbc.jdbc2.optional.{MysqlDataSource}
import org.powerscala.log.Logging
import org.powerscala.property.Property
import org.scalarelational.model._

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class MariaDBConfig(host: String,
                         schema: String,
                         user: String,
                         password: String,
                         profileSQL: Boolean = false,
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

  Class.forName("com.mysql.jdbc.Driver")

  val config = Property[MariaDBConfig]()

  init()

  protected def init() = {
    config.change.on {
      case evt => updateDataSource() // Update the data source if the mode changes
    }
  }

  def updateDataSource() = {
    dispose() // Make sure to shut down the previous DataSource if possible
    val source: MysqlDataSource = new MysqlDataSource()
    source.setURL("jdbc:mysql://" + config().host + "/" + config().schema)
    source.setUser(config().user)
    source.setPassword(config().password)
    source.setPort(config().port)
    source.setProfileSQL(config().profileSQL)
    dataSourceProperty := source
  }
}
