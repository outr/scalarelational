package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ConnectType private() extends EnumEntry

object ConnectType extends Enumerated[ConnectType] {
  val And = new ConnectType
  val Or = new ConnectType
}