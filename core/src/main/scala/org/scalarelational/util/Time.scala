package org.scalarelational.util

object Time {
  /**
    * Waits for <code>condition</code> to return true. This method will wait
    * <code>time</code> (in seconds) for the condition and will return false
    * if the condition is not met within that time. Further, a negative value
    * for <code>time</code> will cause the wait to occur until the condition
    * is true.
    *
    * @param time
    *              The time to wait for the condition to return true.
    * @param precision
    *              The recycle period between checks. Defaults to 0.01s.
    * @param start
    *              The start time in milliseconds since epoc. Defaults to
    *              System.currentTimeMillis.
    * @param errorOnTimeout
    *              If true, throws a java.util.concurrent.TimeoutException upon
    *              timeout. Defaults to false.
    * @param condition
    *              The functional condition that must return true.
    */
  @scala.annotation.tailrec
  def waitFor(time: Double,
              precision: Double = 0.01,
              start: Long = System.currentTimeMillis,
              errorOnTimeout: Boolean = false)
             (condition: => Boolean): Boolean = {
    val p = math.round(precision * 1000.0)
    if (!condition) {
      if ((time >= 0.0) && (System.currentTimeMillis - start > millis(time))) {
        if (errorOnTimeout) throw new java.util.concurrent.TimeoutException()
        false
      } else {
        Thread.sleep(p)

        waitFor(time, precision, start, errorOnTimeout)(condition)
      }
    } else {
      true
    }
  }

  /**
    * Invokes the wrapped function and returns the time in seconds it took to complete as a Double.
    */
  def elapsed(f: => Any): Double = {
    val time = System.nanoTime
    f
    (System.nanoTime - time) / 1000000000.0
  }

  /**
    * Converts time in seconds to milliseconds.
    */
  def millis(time: Double): Long = math.round(time * 1000.0)
}
