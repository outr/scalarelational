package org.scalarelational

import org.scalarelational.model.Datastore

import scala.concurrent._

trait SessionSupport { this: Datastore =>
  protected def executionContext = ExecutionContext.global

  protected val _session = new ThreadLocal[Session]

  def hasSession: Boolean = _session.get() != null

  protected def createSession(): (Boolean, Session) =
    if (hasSession) {
      (false, _session.get)
    } else {
      val session = instantiateSession()
      _session.set(session)
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

  protected def commitTransaction(session: Session): Unit = {
    session.connection.commit()
    session.connection.setAutoCommit(false)
    session.inTransaction = false
  }

  protected def rollbackTransaction(session: Session): Unit = {
    session.connection.rollback()
    session.connection.setAutoCommit(false)
    session.inTransaction = false
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
      val created = createTransaction(session)
      try {
        val result: Result = f(session)
        if (created) commitTransaction(session)
        result
      } finally {
        if (created) rollbackTransaction(session)
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
