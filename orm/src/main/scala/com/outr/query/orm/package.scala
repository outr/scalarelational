package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object orm {
  private var mappings = Map.empty[Table, ORMTable[_]]

//  implicit def table2ORMTable[T](table: Table)(implicit manifest: Manifest[T]) = synchronized {
//    mappings.get(table) match {
//      case Some(ormt) => ormt.asInstanceOf[ORMTable[T]]
//      case None => {
//        val ormt = new ORMTable[T](table)
//        mappings += table -> ormt
//        ormt
//      }
//    }
//  }

  def orm[T](table: Table)(implicit manifest: Manifest[T]) = synchronized {
    mappings.get(table) match {
      case Some(ormt) => ormt.asInstanceOf[ORMTable[T]]
      case None => {
        val ormt = new ORMTable[T](table)
        mappings += table -> ormt
        ormt
      }
    }
  }
}
