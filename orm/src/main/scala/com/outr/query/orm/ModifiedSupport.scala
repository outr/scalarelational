package com.outr.query.orm

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ModifiedSupport[T] {
  this: ORMTable[T] =>

  persisting.on {
    case value => if (value != null) {
      val persistence = persistenceFor(value.getClass)
      val modifiedCaseValue = persistence.caseValues("modified")
      modifiedCaseValue.copy(value, System.currentTimeMillis())
    } else {
      value
    }
  }
}