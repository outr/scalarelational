package org.scalarelational.model2.query

import org.scalarelational.model2._

import scala.collection.mutable.ListBuffer

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query[G <: Group[Field[_]]](fields: G,
                                       table: Table,
                                       alias: Option[String] = None,
                                       joins: List[Join] = Nil) extends ModelEntry with Joinable {
  def apply[T](field: Field[T]) = new ReferenceField[T](field, alias)

  def as(alias: String) = copy(alias = Some(alias))

  def join(joinable: Joinable, joinType: JoinType = JoinType.Join) = PartialJoin[G](this, joinable, joinType)
  def innerJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Inner)

  override def toSQL = {
    val fieldsSQL = fields.items.map(_.toSQL)
    val tableSQL = table.toSQL

    val args = ListBuffer.empty[Any]

    fieldsSQL.foreach(sql => args ++= sql.args)
    args ++= tableSQL.args

    val b = new StringBuilder
    if (alias.nonEmpty) b.append("(")
    b.append("SELECT(")
    b.append(fieldsSQL.map(_.text).mkString(", "))
    b.append(") FROM ")
    b.append(tableSQL.text)
    joins.foreach {
      case join => {
        val joinSQL = join.toSQL
        args ++= joinSQL.args
        b.append(s" ${joinSQL.text}")
      }
    }
    alias match {
      case Some(a) => {
        b.append(" AS [")
        b.append(a)
        b.append("])")
      }
      case None => // No alias
    }

    SQL(b.toString(), fieldsSQL.map(_.args) ::: List(tableSQL.args))
  }
}