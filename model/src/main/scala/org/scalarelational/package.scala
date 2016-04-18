package org

import scala.language.implicitConversions

package object scalarelational {
  implicit def database2SessionSupport[D <: Database](database: D): DatabaseSessionSupport[D] = {
    DatabaseSessionSupport(database)
  }
  implicit def database2DDLSupport[D <: Database](database: D): DatabaseDDLSupport[D] = {
    DatabaseDDLSupport(database)
  }
}