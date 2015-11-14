package org.scalarelational.extra

import org.scalarelational.model.Datastore
import org.scalarelational.{Session, SessionSupport}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait StickySessionSupport extends SessionSupport {
  this: Datastore =>

  def sessionTimeout: Double = 5.0

  private val stickySession = new ThreadLocal[StickySession]

  override protected def disposeSession() = {
    if (!hasSession) throw new RuntimeException(s"No context currently exists in current thread...cannot dispose.")
    assert(stickySession.get() == null, "Sticky Session should never be set when dispose is called.")
    if (session.hasConnection) {
      val ss = StickySession(session)
      stickySession.set(StickySession(ss.session))
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
      super.disposeSession()
    }
  }

  override protected def instantiateSession() = stickySession.get() match {
    case null => super.instantiateSession()
    case ss => {
      ss.cancelled = true
      stickySession.remove()
      ss.session
    }
  }
}

case class StickySession(session: Session) {
  var cancelled = false
}