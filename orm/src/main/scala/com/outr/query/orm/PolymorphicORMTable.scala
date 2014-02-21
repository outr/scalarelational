package com.outr.query.orm

import com.outr.query.{QueryResult, Column, Datastore}
import com.outr.query.table.property.TableProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class PolymorphicORMTable[T](datastore: Datastore, name: String, tableProperties: TableProperty*)(implicit manifest: Manifest[T])
                                      extends ORMTable[T](datastore, name, tableProperties: _*)(manifest) {
  def this(datastore: Datastore, tableProperties: TableProperty*)(implicit manifest: Manifest[T]) = this(datastore, null.asInstanceOf[String], tableProperties: _*)

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