package com.outr.query.search

import com.outr.query.Table
import com.outr.query.h2.H2Datastore

/**
 * SearchSupport adds support for indexed elements to be updated with Apache Lucene search indexing via database
 * triggers in H2.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait SearchSupport extends H2Datastore {
  override def createTableExtras(table: Table, b: StringBuilder) = {
    super.createTableExtras(table, b)


  }
}