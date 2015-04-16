package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed abstract class ConnectType extends EnumEntry

object ConnectType extends Enumerated[ConnectType] {
  case object And extends ConnectType
  case object Or extends ConnectType

  val values = findValues.toVector
}