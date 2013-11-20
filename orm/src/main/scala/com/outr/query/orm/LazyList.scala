package com.outr.query.orm

import com.outr.query.{Column, Query}
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait LazyList[T] extends (() => List[T]) {
  LazyList

  lazy val clazz: EnhancedClass = manifest.runtimeClass
  if (clazz.javaClass == classOf[AnyRef]) {
    throw new RuntimeException("Cannot set LazyList generic type to AnyRef.")
  }

  def manifest: Manifest[T]

  def loaded: Boolean
}

object LazyList {
//  ORMTable.persistenceSupport.listen(Priority.Low) {    // Support lazy list converter
//    case persistence => if (persistence.column == null && persistence.caseValue.valueType.hasType(classOf[LazyList[_]])) {
//      persistence.copy(converter = new LazyListConverter(persistence.table.asInstanceOf[ORMTable[Any]], persistence.caseValue))
//    } else {
//      persistence
//    }
//  }

  def connect[Origin, ListItem, C](table: ORMTable[Origin], fieldName: String, foreignColumn: Column[C])
                                  (implicit manifest: Manifest[ListItem]) = {
    val caseValue = table.clazz.caseValue(fieldName).getOrElse(throw new RuntimeException(s"Unable to find '$fieldName' in ${table.clazz.name}"))
    new LazyListConnectionO2M[Origin, ListItem, C](table, caseValue, foreignColumn, manifest)
  }

  def apply[T](values: T*)(implicit manifest: Manifest[T]) = PreloadedLazyList[T](values.toList)

  def Empty[T](implicit manifest: Manifest[T]) = PreloadedLazyList[T](Nil)
}

case class PreloadedLazyList[T](values: List[T])(implicit val manifest: Manifest[T]) extends LazyList[T] {
  def loaded = true

  def apply() = values
}

case class DelayedLazyList[T](table: ORMTable[T], query: Query)(implicit val manifest: Manifest[T]) extends LazyList[T] {
  @volatile private var _loaded = false
  @volatile private var values: List[T] = null
  def loaded = _loaded

  def apply() = synchronized {
    if (!loaded) {
      load()
    }
    values
  }

  private def load() = {
//    val query = Query(table.*, table).where(conditions)
    values = table.query(query).toList
    _loaded = true
  }
}

class LazyListConnectionO2M[Origin, ListItem, C](table: ORMTable[Origin],
                                                 caseValue: CaseValue,
                                                 foreignColumn: Column[C],
                                                 listItemManifest: Manifest[ListItem]) {
  val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[ListItem]]

  // After persisting, persist entries and update LazyList with ids
  table.persisted.on {
    case origin => {
      val lzy = caseValue[LazyList[ListItem]](origin.asInstanceOf[AnyRef])
      lzy match {
        case l: PreloadedLazyList[ListItem] => {
          val list = l().map(item => foreignTable.persist(item))
          val updated = PreloadedLazyList[ListItem](list)(listItemManifest)
          caseValue.copy(origin, updated)
        }
        case l: DelayedLazyList[ListItem] => origin
      }
    }
  }

  // After querying update the instance with LazyList reference
  table.queried.on {
    case origin => table.idFor[C](origin) match {
      case Some(idColumnValue) => {
        val id = idColumnValue.value
        val query = foreignTable.q where foreignColumn === id
        val lzy = DelayedLazyList[ListItem](foreignTable, query)(listItemManifest)
        caseValue.copy(origin, lzy)
      }
      case None => origin   // No id found for the queried object, perhaps not part of the query
    }
  }
}