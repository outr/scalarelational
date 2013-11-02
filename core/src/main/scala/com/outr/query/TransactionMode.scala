package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TransactionMode private(val value: Int) extends EnumEntry {

}

object TransactionMode extends Enumerated[TransactionMode] {
  /**
   * A constant indicating that transactions are not supported.
   */
  val None = new TransactionMode(0)
  /**
   * A constant indicating that
   * dirty reads, non-repeatable reads and phantom reads can occur.
   * This level allows a row changed by one transaction to be read
   * by another transaction before any changes in that row have been
   * committed (a "dirty read").  If any of the changes are rolled back,
   * the second transaction will have retrieved an invalid row.
   */
  val ReadUncommitted = new TransactionMode(1)
  /**
   * A constant indicating that
   * dirty reads are prevented; non-repeatable reads and phantom
   * reads can occur.  This level only prohibits a transaction
   * from reading a row with uncommitted changes in it.
   */
  val ReadCommitted = new TransactionMode(2)
  /**
   * A constant indicating that
   * dirty reads and non-repeatable reads are prevented; phantom
   * reads can occur.  This level prohibits a transaction from
   * reading a row with uncommitted changes in it, and it also
   * prohibits the situation where one transaction reads a row,
   * a second transaction alters the row, and the first transaction
   * rereads the row, getting different values the second time
   * (a "non-repeatable read").
   */
  val RepeatableRead = new TransactionMode(4)
  /**
   * A constant indicating that
   * dirty reads, non-repeatable reads and phantom reads are prevented.
   * This level includes the prohibitions in
   * <code>TRANSACTION_REPEATABLE_READ</code> and further prohibits the
   * situation where one transaction reads all rows that satisfy
   * a <code>WHERE</code> condition, a second transaction inserts a row that
   * satisfies that <code>WHERE</code> condition, and the first transaction
   * rereads for the same condition, retrieving the additional
   * "phantom" row in the second read.
   */
  val Serializable = new TransactionMode(8)

  def byValue(value: Int) = values.find(m => m.value == value).getOrElse(throw new RuntimeException(s"Unable to find TransactionMode by value: $value"))
}