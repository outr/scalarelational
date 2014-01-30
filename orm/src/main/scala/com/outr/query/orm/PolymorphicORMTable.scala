package com.outr.query.orm

import com.outr.query.{QueryResult, Column, Datastore}

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class PolymorphicORMTable[T](tableName: String)(implicit manifest: Manifest[T], datastore: Datastore)
                                      extends ORMTable[T](tableName)(manifest, datastore) {
  def caseClasses: Vector[Class[_ <: T]]
  def typeColumn: Column[Int]

  private def typeFromInstance(t: T) = caseClasses.indexOf(t.getClass) match {
    case -1 => throw new NullPointerException(s"Case Class Not Found for type: ${t.getClass}")
    case index => index + 1
  }

  override def object2Row(instance: T, onlyChanges: Boolean) = {
    val mapped = super.object2Row(instance, onlyChanges)
    mapped.copy(columnValues = typeColumn(typeFromInstance(instance)) :: mapped.columnValues)
  }

  override def caseClassForRow(result: QueryResult) = caseClasses(result(typeColumn) - 1)
}