package org.scalarelational


trait PropertyContainer[P <: Prop] {
  private var _properties = Map.empty[String, P]

  def properties = _properties

  final def has(property: P): Boolean = has(property.name)
  final def has(propertyName: String): Boolean = properties.contains(propertyName)
  final def get[T <: P](propertyName: String) = properties.get(propertyName).asInstanceOf[Option[T]]
  final def prop[T <: P](propertyName: String) = get[T](propertyName).getOrElse(throw new NullPointerException(s"Unable to find property by name '$propertyName' in ${getClass.getSimpleName} $this."))

  /**
   * Adds the supplied properties to this container.
   *
   * @param properties the properties to add
   * @return this
   */
  def props(properties: P*) = synchronized {
    properties.foreach {
      case p => _properties += p.name -> p
    }
    this
  }
}