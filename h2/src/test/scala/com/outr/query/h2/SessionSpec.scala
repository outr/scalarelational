package com.outr.query.h2

import org.specs2.mutable.Specification
import org.powerscala.concurrent.Time

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SessionSpec extends Specification {
  import TestDatastore._

  "session" should {
    "wait for no all sessions to terminate" in {
      Time.waitFor(10.0) {
        sessions.isEmpty
      } mustNotEqual false
    }
    "create a single session" in {
      create()
      sessions.nonEmpty mustEqual true
    }
    "timeout after 5 seconds when idling" in {
      sessions.nonEmpty mustEqual true
      Time.waitFor(10.0) {
        sessions.isEmpty
      } mustNotEqual false
    }
    "keep a connection alive for several seconds with queries" in {
      create()      // Initialize our session
      val session = sessions.head
      val query = select(test.*) from test
      (0 until 30).foreach {
        case index => {
          exec(query).toList.size mustEqual 0
          session.disposed mustEqual false
          Time.sleep(1.0)
        }
      }
      sessions.size mustEqual 1
      session.disposed mustEqual false
    }
    "keep a connection alive with 'active' for several seconds" in {
      sessions.size mustEqual 1
      val session = sessions.head
      active {
        Time.sleep(30.0)
      }
      session.disposed mustEqual false
    }
    "timeout sessions before terminating" in {
      Time.waitFor(10.0) {
        sessions.isEmpty
      } mustNotEqual false
    }
  }
}
