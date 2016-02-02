package org.scalarelational.h2

import javax.sql.DataSource

import org.h2.jdbcx.JdbcConnectionPool
import org.scalarelational.Database

import scala.util.Random

class H2Database(val dataSource: DataSource) extends Database {
  def this(mode: H2ConnectionMode = H2Memory(Random.alphanumeric.take(10).toString()),
           username: String = "sa",
           password: String = "sa") {
    this(JdbcConnectionPool.create(mode.url, username, password))
  }
}