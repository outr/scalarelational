package org.scalarelational.mysql

;

import javax.sql.DataSource


import com.mysql.jdbc.jdbc2.optional.{MysqlDataSource}
import org.powerscala.log.Logging
import org.powerscala.property.Property
import org.scalarelational.model._

/**
 * @author Matt Hicks <matt@outr.com>
 */

trait MysqlConnectionMode {
  def url: String
}

object MysqlConnectionMode {
  def apply(connectionURL: String) = new MysqlConnectionMode {
    override def url = connectionURL
  }
}


abstract class MysqlDatastore private() extends SQLDatastore with Logging {

  override def DefaultVarCharLength = 100

  protected def this(url: String,
                     dbUser: String = "sa",
                     dbPassword: String = "sa") = {
    this()

    username := dbUser
    password := dbPassword
    modeProperty := MysqlConnectionMode(url)

  }

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := dataSource
  }

  Class.forName("com.mysql.jdbc.Driver")

  val modeProperty = Property[MysqlConnectionMode]()
  val url = Property[String](default = Some("sa"))
  val username = Property[String](default = Some("sa"))
  val password = Property[String](default = Some("sa"))

  init()

  protected def init() = {
    modeProperty.change.on {
      case evt => updateDataSource() // Update the data source if the mode changes
    }
  }

  def updateDataSource() = {
    dispose() // Make sure to shut down the previous DataSource if possible
    val source: MysqlDataSource = new MysqlDataSource()
    source.setURL(modeProperty().url)
    source.setUser(username())
    source.setPassword(password())
    dataSourceProperty := source
  }


  override def dispose() = {
    super.dispose()

    dataSourceProperty.get match {
      case Some(ds) => ds match {
        //TODO: destroy data source
        case pool: MysqlDataSource => {}
        case _ => // Not a JdbcConnectionPool
      }
      case None => // No previous dataSource
    }
  }
}