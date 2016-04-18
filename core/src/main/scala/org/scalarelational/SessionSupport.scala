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
      val alreadyInTransaction = session.inTransaction
      if (alreadyInTransaction) {
        f(session)
      } else {
        val originalAutoCommit = session.connection.getAutoCommit
        try {
          session.inTransaction = true
          session.connection.setAutoCommit(false)
          val r: Result = f(session)
          session.connection.commit()
          r
        } catch {
          case t: Throwable => {
            session.connection.rollback()
            throw t
          }
        } finally {
          session.inTransaction = false
          session.connection.setAutoCommit(originalAutoCommit)
        }
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
