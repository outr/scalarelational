package org.scalarelational

import javax.sql.DataSource

trait Database {
  def dataSource: DataSource
}