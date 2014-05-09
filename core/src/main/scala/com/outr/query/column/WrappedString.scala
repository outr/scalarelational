package com.outr.query.column

/**
 * WrappedString trait gives the ability create a wrapper type for a String that offers validations, modifications, etc.
 * to a String, but the 'value' field is what is stored in the database as a String.
 *
 * Instances of this class should be case classes that define "value" as one of the fields.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait WrappedString {
  def value: String
}
