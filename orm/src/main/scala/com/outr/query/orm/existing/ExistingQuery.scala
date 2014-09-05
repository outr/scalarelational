package com.outr.query.orm.existing

import com.outr.query.Datastore
import java.sql.ResultSet
import org.powerscala.reflect._

import scala.annotation.tailrec

/**
 * ExistingQuery allows mapping of an existing query to a result result of type R.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ExistingQuery[R](datastore: Datastore, queryString: String)(implicit manifest: Manifest[R]) {
  private val caseClass: EnhancedClass = manifest.runtimeClass
  if (!caseClass.isCase) throw new RuntimeException(s"$caseClass is not a case class!")
  private val caseValues = caseClass.caseValues.map(cv => cv.name.toLowerCase -> cv).toMap

  def query(args: List[Any]) = {
    val namedArgs = args.collect {
      case na: NamedArgument => na
    }
    val query = applyNamed(queryString, namedArgs)
    val results = datastore.session.executeQuery(query, args.filterNot(a => a.isInstanceOf[NamedArgument]))
    new Iterator[R] {
      def hasNext = results.next()
      def next() = result2R(results)
    }.toStream
  }

  private def result2R(result: ResultSet): R = {
    val meta = result.getMetaData
    val args = (0 until meta.getColumnCount).map {
      case index => {
        val name = meta.getColumnLabel(index + 1).toLowerCase
        caseValues.get(name).map(cv => cv.name -> result.getObject(index + 1))
      }
    }.flatten.toMap
    caseClass.create(args)
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

case class NamedArgument(key: String, value: String)