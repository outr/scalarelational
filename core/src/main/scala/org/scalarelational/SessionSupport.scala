package org.scalarelational

import org.scalarelational.model.Datastore

import scala.concurrent._

trait SessionSupport { this: Datastore =>
  protected val sessionLocal: ThreadLocal[SessionStore] = new ThreadLocal[SessionStore] {
    override def initialValue(): SessionStore = new SessionStore()
  }

  def hasSession: Boolean = sessionLocal.get().session.nonEmpty

  protected def createSession(): (Boolean, SessionStore) = {
    val store = sessionLocal.get()
    val created = if (store.session.isEmpty) {
      store.session = Some(instantiateSession())
      true
    } else {
      false
    }
    created -> store
  }

  protected def instantiateSession(): Session = Session(this)

  protected def disposeSession(store: SessionStore): Unit = {
    store.session.foreach(_.dispose())
    store.session = None
  }

  def withSession[Result](f: Session => Result): Result = {
    val (created, store) = createSession()
    try {
      f(store.session.get)
    } finally {
      if (created) disposeSession(store)
    }
  }

  def withAsyncSession[Result](f: Session => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    val (created, store) = createSession()
    val future = f(store.session.get)
    future.onComplete { _ =>
      if (created) disposeSession(store)
    }
    future
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
  )(scala.concurrent.ExecutionContext.global)
}

class SessionStore(var session: Option[Session] = None)