package org.scalarelational

import org.scalarelational.model.Datastore

import scala.concurrent._

trait SessionSupport { this: Datastore =>
  protected def executionContext = ExecutionContext.global

  protected val _session = new ThreadLocal[Option[Session]] {
    override def initialValue = None
  }

  def hasSession: Boolean = _session.get().nonEmpty

  protected def createSession(): (Boolean, Session) =
    _session.get() match {
      case Some(session) => (false, session)
      case None =>
        val session = instantiateSession()
        _session.set(Some(session))
        (true, session)
    }

  protected def instantiateSession(): Session = Session(this)

  protected def disposeSession(session: Session): Unit = {
    if (!hasSession) throw new RuntimeException(
      s"No context currently exists in current thread...cannot dispose.")
    session.dispose()
    _session.remove()
  }

  protected def createTransaction(session: Session): Boolean =
    if (session.inTransaction) {
      false
    } else {
      session.connection.setAutoCommit(false)
      session.inTransaction = true
      true
    }

  def withSession[Result](f: Session => Result): Result = {
    val (created, session) = createSession()
    try {
      f(session)
    } finally {
      if (created) disposeSession(session)
    }
  }

  def transaction[Result](f: Session => Result): Result =
    withSession { session =>
      val originalAutoCommit = session.connection.getAutoCommit
      val created = createTransaction(session)
      try {
        val result: Result = f(session)
        if (created) session.connection.commit()
        result
      } catch { case t: Throwable =>
        session.connection.rollback()
        throw t
      } finally {
        session.connection.setAutoCommit(originalAutoCommit)
        session.inTransaction = false
      }
    }

  /**
   * Executes the inline function asynchronously and surrounds in a session
   * returning Future[Result].
   */
  def async[Result](f: Session => Result): Future[Result] = Future(
    withSession { session =>
      f(session)
    }
  )(executionContext)
}
