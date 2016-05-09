package org.scalarelational.postgresql

import java.sql.Types
import javax.sql.DataSource

import org.postgresql.ds.PGSimpleDataSource
import org.scalarelational.Session
import org.scalarelational.column.property.{AutoIncrement, Default, Polymorphic, Unique}
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.datatype._
import org.scalarelational.instruction.ddl.CreateColumn
import org.scalarelational.model._
import org.scalarelational.op.{Condition, DirectCondition, Operator, RegexCondition}
import org.scalarelational.result.ResultSetIterator
import pl.metastack.metarx.Opt

import scala.collection.mutable.ListBuffer

abstract class PostgreSQLDatastore private() extends SQLDatastore with SQLLogging {
  protected def this(pgConfig: PostgreSQL.Config) = {
    this()
    config := pgConfig
  }

  override def supportsMerge: Boolean = false

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := dataSource
  }

  Class.forName("org.postgresql.Driver")

  val config = Opt[PostgreSQL.Config]()

  // Update the data source if the mode changes
  config.values.attach(updateDataSource)

  private lazy val blobSQLType = new BlobSQLType("BYTEA")

  override protected def catalog: Option[String] = None

  override def dataTypeForInstance[T, S](dataTypeInstance: DataTypeInstance[T, S]): DataType[T, S] = {
    if (dataTypeInstance.dataType.jdbcType == Types.BLOB) {
      dataTypeInstance.dataType.copy(sqlType = blobSQLType)
    } else {
      super.dataTypeForInstance(dataTypeInstance)
    }
  }

  def updateDataSource(config: PostgreSQL.Config): Unit = {
    dispose() // Make sure to shut down the previous DataSource if possible

    val source = new PGSimpleDataSource()
    source.setPortNumber(config.port)
    source.setServerName(config.host)
    source.setDatabaseName(config.schema)
    source.setUser(config.user)
    source.setPassword(config.password)
    config.ssl.foreach { s =>
      source.setSsl(true)
      s.sslFactory.foreach { source.setSslfactory }
      s.sslFactoryArg.foreach { source.setSslFactoryArg }
    }
    dataSourceProperty := source
  }

  override protected def columnPropertiesSQL(container: ColumnPropertyContainer): List[String] = {
    val b = ListBuffer.empty[String]
    if (!container.optional && !container.has(Polymorphic)) {
      b.append("NOT NULL")
    }
    if (container.has(Unique)) {
      b.append("UNIQUE")
    }
    container.get[Default](Default.name) match {
      case Some(default) => b.append(s"DEFAULT ${default.value}")
      case None => // No default specified
    }
    b.toList
  }

  override protected def columnSQLType(create: CreateColumn[_, _]) =
    if (create.has(AutoIncrement)) {
      "SERIAL"
    } else if (create.dataType == DataTypes.DoubleType) {
      "DOUBLE PRECISION"
    } else {
      super.columnSQLType(create)
    }

  override def jdbcTables(implicit session: Session): Set[String] = {
    val meta = session.connection.getMetaData
    val results = meta.getTables(null, "public", "%", null)
    try {
      new ResultSetIterator(results).map(_.getString("TABLE_NAME")).toSet
    } finally {
      results.close()
    }
  }

  override def jdbcColumns(tableName: String)(implicit session: Session): Set[String] = {
    val meta = session.connection.getMetaData
    val results = meta.getColumns(null, "public", tableName, null)
    try {
      new ResultSetIterator(results).map(_.getString("COLUMN_NAME")).toSet
    } finally {
      results.close()
    }
  }

  override def tableExists(name: String)(implicit session: Session): Boolean = {
    val meta = session.connection.getMetaData
    val results = meta.getTables(null, "public", name.toLowerCase, null)
    try {
      results.next()
    } finally {
      results.close()
    }
  }

  override def condition2String(condition: Condition,
                                args: ListBuffer[TypedValue[_, _]]): String =
    condition match {
      case c: RegexCondition[_, _] => {
        args += DataTypes.StringType.typed(c.regex.toString())
        s"${c.column.longName} ${if (c.not) "!~ " else ""}~ ?"
      }

      case c: DirectCondition[_, _] => {
        val dataType = c.column.dataType.asInstanceOf[DataType[Any, Any]]
        val op = dataType.sqlOperator(c.column.asInstanceOf[ColumnLike[Any, Any]], c.value, c.operator)
        op match {
          case Operator.Is | Operator.IsNot => s"${c.column.longName} ${op.symbol} NULL"
          case _ => super.condition2String(condition, args)
        }
      }
      case _ => super.condition2String(condition, args)
    }
}