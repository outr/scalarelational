package org.scalarelational.mapper

import org.scalarelational.model.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait MapTo[MappedType, Id] {
  this: Table =>

  def manifest: Manifest[MappedType]

  def id: Column[Id]

  def query = q.to[MappedType](manifest)

  def byId(id: Id) = datastore.session {
    val q = query where this.id === id
    q.result.converted.headOption
  }
}