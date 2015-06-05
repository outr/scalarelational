package com.outr.query.h2

import java.sql.Timestamp

import com.outr.query.Table
import com.outr.query.column.property.{Unique, AutoIncrement, PrimaryKey}
import org.powerscala.concurrent.Time
import org.scalatest.{Ignore, Matchers, WordSpec}



object TestSessionDatastore extends H2Datastore(mode = H2Memory("sessionspec")) {
  def test = TestSessionTable
}

object TestSessionTable extends Table(TestSessionDatastore) {
  val id = column[Int]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name", Unique)
  val date = column[Timestamp]("date")
}