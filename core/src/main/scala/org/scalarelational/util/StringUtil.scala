package org.scalarelational.util

import scala.util.Random

object StringUtil {
  val DefaultRandomStringLength = 16

  def randomString(length: Int = DefaultRandomStringLength): String = Random.alphanumeric.take(length).mkString

  /**
    * Generates a human readable label for this name.
    */
  def generateLabel(name: String): String = {
    val b = new StringBuilder
    var p = ' '
    name.foreach {
      case '$' => // Ignore $
      case c => {
        if (b.length > 1 && (p.isUpper || p.isDigit) && (!c.isUpper && !c.isDigit)) {
          b.insert(b.length - 1, ' ')
        }
        b.append(c)
        p = c
      }
    }
    b.toString().capitalize
  }
}
