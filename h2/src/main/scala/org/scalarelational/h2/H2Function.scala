package org.scalarelational.h2

import java.sql.ResultSet

import org.scalarelational.Session
import org.scalarelational.util.StringUtil

case class H2Function(datastore: H2Datastore, obj: AnyRef, methodName: String, functionName: Option[String] = None) {
  lazy val name = functionName match {
    case Some(fn) => fn
    case None => StringUtil.generateLabel(methodName).toUpperCase.replace(' ', '_')
  }

  def apply[F](caller: H2Function => F): F = caller(this)

  private def buildStatement(args: Any*)(implicit session: Session) = {
    val argEntries = (0 until args.length).map(i => "?").mkString(", ")
    val s = session.connection.prepareCall(s"CALL ${name}($argEntries)")
    args.zipWithIndex.foreach {
      case (arg, index) => s.setObject(index + 1, arg)
    }
    s
  }

  def call(args: Any*): Boolean = datastore.withSession { implicit session =>
    buildStatement(args: _*).execute()
  }

  def query(args: Any*): ResultSet = datastore.withSession { implicit session =>
    buildStatement(args: _*).executeQuery()
  }
}