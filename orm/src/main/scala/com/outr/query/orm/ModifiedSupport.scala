package com.outr.query.orm

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ModifiedSupport[T] {
  this: ORMTable[T] =>

  val modifiedCaseValue = clazz.caseValue("modified") match {
    case Some(cv) => if (cv.valueType.hasType(classOf[Long])) {
      cv
    } else {
      throw new RuntimeException(s"""Case Class ($clazz) must contain a "modified" field of type Long (type is actually: ${cv.valueType}) to utilize the ModifiedSupport trait.""")
    }
    case None => throw new RuntimeException(s"""Case Class ($clazz) must contain a "modified" field to utilize the ModifiedSupport trait.""")
  }

  persisting.on {
    case value => modifiedCaseValue.copy(value, System.currentTimeMillis())
  }
}