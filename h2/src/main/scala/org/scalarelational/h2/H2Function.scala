package org.scalarelational.h2

import org.powerscala.StringUtil


case class H2Function(datastore: H2Datastore, obj: AnyRef, methodName: String, functionName: Option[String] = None) {
  lazy val name = functionName match {
    case Some(fn) => fn
    case None => StringUtil.generateLabel(methodName).toUpperCase.replace(' ', '_')
  }

  def apply[F](caller: H2Function => F) = caller(this)

  private def buildStatement(args: Any*) = {
    val argEntries = (0 until args.length).map(i => "?").mkString(", ")
    val s = datastore.session.connection.prepareCall(s"CALL ${name}($argEntries)")
    args.zipWithIndex.foreach {
      case (arg, index) => s.setObject(index + 1, arg)
    }
    s
  }

  def call(args: Any*) = datastore.withSession { implicit session =>
    buildStatement(args: _*).execute()
  }

  def query(args: Any*) = datastore.withSession { implicit session =>
    buildStatement(args: _*).executeQuery()
  }
}