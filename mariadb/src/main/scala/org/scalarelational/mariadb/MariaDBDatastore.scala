package org.scalarelational.mariadb

import java.sql.{Types, Blob}
import javax.sql.DataSource
import javax.sql.rowset.serial.SerialBlob

import com.mysql.jdbc.jdbc2.optional.{MysqlDataSource}
import org.powerscala.log.Logging
import org.powerscala.property.Property
import org.scalarelational.model._
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, SQLConversion, ObjectSQLConverter}
import org.scalarelational.instruction.CallableInstruction
import org.scalarelational.instruction.ddl.DropTable

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
  /* This is kind of abitrary, but `DataSource.DefaultVarCharLength` does not
   * work here as it is the row size limit for MariaDB */
  override def DefaultVarCharLength = 200

  dataTypeInstanceProcessor.on { instance =>
    instance.dataType match {
      case DataType(Types.BLOB, _, c, _) if c.isInstanceOf[ObjectSQLConverter[_]] => {
        instance.dataType.copy(converter = new SQLConversion[Any, Array[Byte]] {
          override def toSQL(column: ColumnLike[Any, Array[Byte]], value: Any) = {
            val anyColumn = column.asInstanceOf[ColumnLike[Any, Any]]
            val blob = instance.dataType.converter.toSQL(anyColumn, value).asInstanceOf[Blob]
            blob.getBytes(1, blob.length.asInstanceOf[Int])
          }
          override def fromSQL(column: ColumnLike[Any, Array[Byte]], value: Array[Byte]) = {
            val blob = new SerialBlob(value)
            val anyColumn = column.asInstanceOf[ColumnLike[Any, Any]]
            instance.dataType.converter.fromSQL(anyColumn, blob)
          }
        }.asInstanceOf[SQLConversion[Any, Any]])
      }
      case DataType(Types.BLOB, _, _, _) => {
        instance.dataType.copy(converter = new SQLConversion[SerialBlob, Array[Byte]] {
          override def toSQL(column: ColumnLike[SerialBlob, Array[Byte]], value: SerialBlob) = value.getBytes(1, value.length.asInstanceOf[Int])
          override def fromSQL(column: ColumnLike[SerialBlob, Array[Byte]], value: Array[Byte]) = new SerialBlob(value)
        }.asInstanceOf[SQLConversion[Any, Any]])
      }
      case _ => instance.dataType
    }
  }

  Class.forName("com.mysql.jdbc.Driver")

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
    val source: MysqlDataSource = new MysqlDataSource()
    source.setURL("jdbc:mysql://" + config().host + "/" + config().schema)
    source.setUser(config().user)
    source.setPassword(config().password)
    source.setPort(config().port)
    source.setProfileSQL(config().profileSQL)
    dataSourceProperty := source
  }
}
