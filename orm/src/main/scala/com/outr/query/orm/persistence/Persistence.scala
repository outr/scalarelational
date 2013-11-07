package com.outr.query.orm.persistence

import org.powerscala.reflect.CaseValue
import com.outr.query.Column
import com.outr.query.orm.ORMTable

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Persistence(table: ORMTable[_],
                       caseValue: CaseValue,
                       column: Column[_] = null,
                       converter: Converter = null)