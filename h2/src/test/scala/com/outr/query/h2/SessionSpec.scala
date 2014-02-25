package com.outr.query.h2

import org.powerscala.concurrent.Time
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SessionSpec extends WordSpec with Matchers {
  import TestDatastore._

  "session" should {
    "wait for all sessions to terminate" in {
      Time.waitFor(10.0) {
        sessions.isEmpty
      } shouldNot equal(false)
    }
    "create a single session" in {
      create()
      sessions.nonEmpty should equal(true)
    }
    "timeout after 5 seconds when idling" in {
      sessions.nonEmpty should equal(true)
      Time.waitFor(10.0) {
        sessions.isEmpty
      } shouldNot equal(false)
    }
    "keep a connection alive for several seconds with queries" in {
      create()      // Initialize our session
      val session = sessions.head
      val query = select(test.*) from test
      (0 until 30).foreach {
        case index => {
          exec(query).toList.size should equal(0)
          session.disposed should equal(false)
          Time.sleep(1.0)
        }
      }
      sessions.size should equal(1)
      session.disposed should equal(false)
    }
    "keep a connection alive with 'active' for several seconds" in {
      sessions.size should equal(1)
      val session = sessions.head
      active {
        Time.sleep(30.0)
      }
      session.disposed should equal(false)
    }
    "timeout sessions before terminating" in {
      Time.waitFor(10.0) {
        sessions.isEmpty
      } shouldNot equal(false)
    }
  }
}
