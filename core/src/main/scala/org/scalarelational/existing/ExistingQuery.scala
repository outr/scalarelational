package org.scalarelational.existing

import java.sql.ResultSet

import org.scalarelational.Session
import org.scalarelational.datatype.{DataTypes, TypedValue}
import org.scalarelational.model.Database

import scala.annotation.tailrec

/**
 * ExistingQuery allows mapping of an existing query to a result result of type R.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ExistingQuery[R](database: Database,
                       queryString: String,
                       resultConverter: ResultSet => R)
                      (implicit manifest: Manifest[R]) {
  def query(args: List[TypedValue[_, _]])(implicit session: Session): Stream[R] = {
    val namedArgs = args.collect {
      case arg if arg.isInstanceOf[NamedArgument] => arg.asInstanceOf[NamedArgument]
    }
    val query = applyNamed(queryString, namedArgs)
    val results = session.executeQuery(query, args.filterNot(a => a.isInstanceOf[NamedArgument]))
    new Iterator[R] {
      def hasNext = results.next()
      def next() = resultConverter(results)
    }.toStream
  }

  @tailrec
  private def applyNamed(q: String, args: List[NamedArgument]): String = if (args.isEmpty) {
    q
  } else {
    val arg = args.head
    val modified = q.replaceAll(s":${arg.key}", arg.value)
    applyNamed(modified, args.tail)
  }
}

class NamedArgument(val key: String, value: String) extends TypedValue[String, String](DataTypes.StringType, value) {
  override def toString: String = s"NamedArgument($key = $value)"
}