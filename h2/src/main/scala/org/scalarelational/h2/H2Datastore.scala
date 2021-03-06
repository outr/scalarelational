package org.scalarelational.h2

import javax.sql.DataSource

import reactify.{Channel, Var}
import org.h2.jdbcx.JdbcConnectionPool
import org.scalarelational.Session
import org.scalarelational.h2.trigger.{TriggerEvent, TriggerType}
import org.scalarelational.model._
import org.scalarelational.table.Table
import org.scalarelational.util.StringUtil

abstract class H2Datastore private() extends SQLDatastore {
  protected def this(mode: H2ConnectionMode = H2Memory(StringUtil.randomString()),
                     username: String = "sa",
                     password: String = "sa") {
    this()
    dbUsername := username
    dbPassword := password
    modeProperty := Some(mode)
  }

  protected def this(dataSource: DataSource) = {
    this()
    dataSourceProperty := Some(dataSource)
  }

  Class.forName("org.h2.Driver")

  val modeProperty: Var[Option[H2ConnectionMode]] = Var(None)
  val dbUsername: Var[String] = Var("sa")
  val dbPassword: Var[String] = Var("sa")
  val trigger: Channel[TriggerEvent] = Channel[TriggerEvent]

  private var functions = Set.empty[H2Function]

  // Update the data source if the mode changes
  modeProperty.attach(updateDataSource)

  override protected def catalog: Option[String] = None

  private def updateDataSource(modeOption: Option[H2ConnectionMode]): Unit = modeOption.foreach { mode =>
    dispose()  // Make sure to shut down the previous DataSource if possible
    dataSourceProperty := Some(JdbcConnectionPool.create(mode.url, dbUsername.get, dbPassword.get))
  }

  def function[F](obj: AnyRef, methodName: String, functionName: Option[String] = None): H2Function = synchronized {
    val f = H2Function(this, obj, methodName, functionName)
    functions += f
    f
  }

  override def create(tables: Table*)(implicit session: Session): Int = {
    val created = super.create(tables: _*)

    // TODO convert this to use CallableInstructions
    val b = new StringBuilder
    tables.foreach {
      case table => createTableTriggers(table, b)
    }

    createFunctions(b)

    if (b.nonEmpty) {
      session.execute(b.toString())
    }

    created
  }

  private def createTableTriggers(table: Table, b: StringBuilder) = if (table.has(Triggers.name)) {
    val triggers = table.get[Triggers](Triggers.name).get
    if (triggers.has(TriggerType.Insert)) {
      b.append(s"""CREATE TRIGGER IF NOT EXISTS ${table.tableName}_INSERT_TRIGGER AFTER INSERT ON ${table.tableName} FOR EACH ROW CALL "org.scalarelational.h2.trigger.TriggerInstance";\r\n\r\n""")
    }
    if (triggers.has(TriggerType.Update)) {
      b.append(s"""CREATE TRIGGER IF NOT EXISTS ${table.tableName}_UPDATE_TRIGGER AFTER UPDATE ON ${table.tableName} FOR EACH ROW CALL "org.scalarelational.h2.trigger.TriggerInstance";\r\n\r\n""")
    }
    if (triggers.has(TriggerType.Delete)) {
      b.append(s"""CREATE TRIGGER IF NOT EXISTS ${table.tableName}_DELETE_TRIGGER AFTER DELETE ON ${table.tableName} FOR EACH ROW CALL "org.scalarelational.h2.trigger.TriggerInstance";\r\n\r\n""")
    }
    if (triggers.has(TriggerType.Select)) {
      b.append(s"""CREATE TRIGGER IF NOT EXISTS ${table.tableName}_SELECT_TRIGGER BEFORE SELECT ON ${table.tableName} CALL "org.scalarelational.h2.trigger.TriggerInstance";\r\n\r\n""")
    }
  }

  private def createFunctions(b: StringBuilder): Unit = functions.foreach {
    case f => b.append(s"""CREATE ALIAS IF NOT EXISTS ${f.name} FOR "${f.obj.getClass.getName.replaceAll("[$]", "")}.${f.methodName}";\r\n\r\n""")
  }

  override protected def disposeDataSource(dataSource: DataSource): Unit = {
    super.disposeDataSource(dataSource)

    dataSource match {
      case ds: JdbcConnectionPool => ds.dispose()
      case _ => // Ignore
    }
  }
}