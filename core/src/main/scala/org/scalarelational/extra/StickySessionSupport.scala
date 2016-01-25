package org.scalarelational.extra

import org.scalarelational.model.Database
import org.scalarelational.{Session, SessionSupport}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try


trait StickySessionSupport extends SessionSupport {
  this: Database =>

  def sessionTimeout: Double = 5.0

  private val stickySession = new ThreadLocal[Option[StickySession]] {
    override def initialValue(): Option[StickySession] = None
  }

  override protected def disposeSession(session: Session): Unit = {
    if (!hasSession) throw new RuntimeException(s"No context currently exists in current thread...cannot dispose.")
    assert(stickySession.get().isEmpty, "Sticky Session should never be set when dispose is called.")
    if (session.hasConnection) {
      val ss = StickySession(session)
      stickySession.set(Some(StickySession(ss.session)))
      _session.remove()

      Future {
        val deadline = sessionTimeout.seconds.fromNow
        Try(Await.ready(Promise().future, deadline.timeLeft))
        if (!ss.cancelled) {
          stickySession.remove()
          ss.session.dispose()
        }
      }
    } else {
      super.disposeSession(session)
    }
  }

  override protected def instantiateSession(): Session =
    stickySession.get() match {
      case None => super.instantiateSession()
      case Some(ss) => {
        ss.cancelled = true
        stickySession.remove()
        ss.session
      }
    }
}

case class StickySession(session: Session) {
  var cancelled = false
}