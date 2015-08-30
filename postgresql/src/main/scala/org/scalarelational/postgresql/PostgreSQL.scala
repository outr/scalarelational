package org.scalarelational.postgresql

/**
 * @author Robert Djubek <envy1988@gmail.com>
 */
object PostgreSQL {
  case class SSL(sslFactory: Option[String] = None,
                 sslFactoryArg: Option[String] = None)

  case class Config(host: String,
                      schema: String,
                      user: String,
                      password: String,
                      port: Int = 5432,
                      ssl: Option[PostgreSQL.SSL] = None)
}