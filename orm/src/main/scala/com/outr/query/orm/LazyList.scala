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
  /**
   * Connects one-to-many relationship for LazyList.
   *
   * @param table the origin table that contains the LazyList
   * @param fieldName the field name representing the LazyList in the case class
   * @param foreignColumn the foreign column for the id of the list items
   * @param manifest manifest for the list item
   * @tparam Origin the originating object type
   * @tparam ListItem the item type for the LazyList
   * @tparam C the column type
   */
  def connect[Origin, ListItem, C](table: ORMTable[Origin], fieldName: String, foreignColumn: Column[C])
                                  (implicit manifest: Manifest[ListItem]): Unit = {
    val caseValue = table.clazz.caseValue(fieldName).getOrElse(throw new RuntimeException(s"Unable to find '$fieldName' in ${table.clazz.name}"))
    new LazyListConnectionO2M[Origin, ListItem, C](table, caseValue, foreignColumn, manifest)
  }

  /**
   * Connects many-to-many relationship for LazyList.
   *
   * This connects both directions so only needs to be called from one perspective.
   *
   * @param originTable the origin table
   * @param originFieldName the field name representing the LazyList in the origin case class
   * @param linkingTableDestinationId the destination id in the linking table
   * @param destinationTable the destination table
   * @param destinationFieldName the field name representing the LazyList in the destination case class
   * @param linkingTableOriginId the origin id in the linking table
   * @param originManifest the manifest for the origin
   * @param destinationManifest the manifest for the destination
   * @tparam Origin the originating object type
   * @tparam Destination the destination object type
   * @tparam C the column type
   */
  def connect[Origin, Destination, C](originTable: ORMTable[Origin],
                                      originFieldName: String,
                                      linkingTableDestinationId: Column[C],
                                      destinationTable: ORMTable[Destination],
                                      destinationFieldName: String,
                                      linkingTableOriginId: Column[C])
                                     (implicit originManifest: Manifest[Origin],
                                      destinationManifest: Manifest[Destination]): Unit = {
    if (originFieldName != null) {
      val originCaseValue = originTable.clazz.caseValue(originFieldName).getOrElse(throw new RuntimeException(s"Unable to find '$originFieldName' in ${originTable.clazz.name}"))
      new LazyListConnectionM2M[Origin, Destination, C](originTable, originCaseValue, linkingTableOriginId, linkingTableDestinationId, destinationTable, destinationManifest)
    }
    if (destinationFieldName != null) {
      val destinationCaseValue = destinationTable.clazz.caseValue(destinationFieldName).getOrElse(throw new RuntimeException(s"Unable to find '$destinationFieldName' in ${destinationTable.clazz.name}"))
      new LazyListConnectionM2M[Destination, Origin, C](destinationTable, destinationCaseValue, linkingTableDestinationId, linkingTableOriginId, originTable, originManifest)
    }
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

class LazyListConnectionM2M[Origin, ListItem, C](table: ORMTable[Origin],
                                                 caseValue: CaseValue,
                                                 linkingTableOrigin: Column[C],
                                                 linkingTableListItem: Column[C],
                                                 foreignTable: ORMTable[ListItem],
                                                 listItemManifest: Manifest[ListItem]) {
  val datastore = table.datastore
  val linkingTable = linkingTableOrigin.table
  val foreignTablePrimaryKey = foreignTable.primaryKeys.head.asInstanceOf[Column[C]]

  // After persisting, persist entries, insert linking records, and update LazyList with ids
  table.persisted.on {
    case origin => if (!LazyListConnectionM2M.persisting.get()) {
      LazyListConnectionM2M.persisting.set(true)
      try {
        val lzy = caseValue[LazyList[ListItem]](origin.asInstanceOf[AnyRef])
        val originId = table.idFor[C](origin).getOrElse(throw new RuntimeException(s"Unable to find id for $origin (${origin.getClass.getName})")).value
        lzy match {
          case l: PreloadedLazyList[ListItem] => {
            // Delete all linking records for this instance
            datastore.exec(datastore.delete(linkingTable) where linkingTableOrigin === originId)

            val list = l().map {
              case item => {
                // Persist the list item
                val updatedItem = foreignTable.persist(item)
                val listItemId = foreignTable.idFor[C](updatedItem).getOrElse(throw new RuntimeException(s"Unable to find id for $updatedItem (${updatedItem.getClass.getName})")).value

                // Insert into the linking table
                datastore.insert(linkingTableOrigin(originId), linkingTableListItem(listItemId))

                updatedItem
              }
            }
            val updated = PreloadedLazyList[ListItem](list)(listItemManifest)
            caseValue.copy(origin, updated)
          }
          case l: DelayedLazyList[ListItem] => origin     // Nothing to do, nothing has changed
        }
      } finally {
        LazyListConnectionM2M.persisting.set(false)
      }
    } else {
      origin
    }
  }

  // After querying update the instance with LazyList reference
  table.queried.on {
    case origin => table.idFor[C](origin) match {
      case Some(idColumnValue) => {
        val id = idColumnValue.value
        val query = foreignTable.q innerJoin linkingTable on foreignTablePrimaryKey === linkingTableListItem where linkingTableOrigin === id
        val lzy = DelayedLazyList[ListItem](foreignTable, query)(listItemManifest)
        caseValue.copy(origin, lzy)
      }
      case None => origin   // No id found for the queried object, perhaps not part of the query
    }
  }
}

object LazyListConnectionM2M {
  private val persisting = new ThreadLocal[Boolean] {
    override def initialValue() = false
  }
}