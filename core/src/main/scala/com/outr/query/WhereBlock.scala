package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait WhereBlock

case class SingleWhereBlock(condition: Condition) extends WhereBlock

case class MultiWhereBlock(blocks: List[WhereBlock], connectType: ConnectType) extends WhereBlock