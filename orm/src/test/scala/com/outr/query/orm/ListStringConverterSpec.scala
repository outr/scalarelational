package com.outr.query.orm

import com.outr.query.orm.convert.ListStringConverter
import org.scalatest.{Matchers, WordSpec}

class ListStringConverterSpec extends WordSpec with Matchers {

  "ListStringConverter" should {
    "properly convert String to List" in {
      val list = ListStringConverter.toORM(null, "aa|bb", null)
      list.get.length should equal(2)
    }
  }
}
