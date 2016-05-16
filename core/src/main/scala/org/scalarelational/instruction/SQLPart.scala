package org.scalarelational.instruction

import org.scalarelational.instruction.args.SQLArgument

/**
  * SQLPart is the base trait utilized for building SQL instructions. A SQLPart corresponds to a String representation
  * along with a argument list.
  */
trait SQLPart {
  def sql: String
  def args: Vector[SQLArgument]

  /**
    * Simplifies merging multiple sub-parts into a single set of arguments.
    *
    * @param parts the sub-parts to be combined
    * @return flattened Vector of arguments
    */
  protected def mergeArgs(parts: SQLPart*): Vector[SQLArgument] = parts.flatMap(_.args).toVector
}