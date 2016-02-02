package org.scalarelational

class DatabaseSessionSupport[D <: Database](database: D) {
  private val sessionLocal = new ThreadLocal[Option[Session[D]]] {
    override def initialValue(): Option[Session[D]] = None
  }

  def withSession[Result](f: Session[D] => Result): Result = {
    val (session, created) = sessionLocal.get() match {
      case Some(s) => (s, false)
      case None => {
        val s = new Session[D](database)
        sessionLocal.set(Option(s))
        (s, true)
      }
    }
    try {
      f(session)
    } finally {
      if (created) {
        sessionLocal.remove()
        session.dispose()
      }
    }
  }

  def transaction[R](f: => R): R = withSession { session =>
    session.withTransaction[R](f)
  }
}

object DatabaseSessionSupport {
  private var map = Map.empty[Database, DatabaseSessionSupport[_]]

  def apply[D <: Database](database: D): DatabaseSessionSupport[D] = synchronized {
    map.get(database) match {
      case Some(dss) => dss.asInstanceOf[DatabaseSessionSupport[D]]
      case None => {
        val dss = DatabaseSessionSupport[D](database)
        map += database -> dss
        dss
      }
    }
  }
}