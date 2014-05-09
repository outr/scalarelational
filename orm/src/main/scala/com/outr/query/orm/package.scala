package com.outr.query

import java.sql.Timestamp

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object orm {
  implicit def long2Timestamp(l: Long) = new Timestamp(l)
  implicit def timestamp2Long(ts: Timestamp) = ts.getTime
}
