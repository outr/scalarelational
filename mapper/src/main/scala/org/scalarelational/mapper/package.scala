package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.instruction.Query
import org.scalarelational.result.QueryResult

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MappableQuery(query: Query) {
    def mapped[R](f: QueryResult => R): Stream[R] = query.result.toStream.map(qr => f(qr))

    def as[R](implicit manifest: Manifest[R]): Stream[R] = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val f = (r: QueryResult) => {
        clazz.create[R](r.toSimpleMap)
      }
      mapped[R](f)
    }
  }
}