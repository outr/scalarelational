# ScalaRelational: Better SQL in Scala

[![Build Status](https://travis-ci.org/outr/scalarelational.svg?branch=master)](https://travis-ci.org/outr/scalarelational)
[![Stories in Ready](https://badge.waffle.io/outr/scalarelational.png?label=ready&title=Ready)](https://waffle.io/outr/scalarelational)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/scalarelational)

**ScalaRelational** was created after evaluating all the existing database frameworks for working with SQL and finding them
both frustrating to work with and overly complex. Most database frameworks attempt to abstract SQL with the goal of
making working with your database easier, but this comes at the cost of inefficient queries, missing features, and
confusing code mapping. The goal of ScalaRelational is to provide a one-to-one representation of the SQL language in
Scala and provide performance improvements to working with the database.

Currently H2 and MySQL is tested, but other databases technically are supported though certain functionality may require
some slight alterations to work properly. One of the goals of ScalaRelational is to be a straight-forward, type-safe framework
to write SQL queries in Scala with no magic or guesswork of what the resulting SQL will be.

It is worth mentioning that much of the functionality of this framework was inspired by other database frameworks like
Slick but there is a great deal of innovation and distinction in ScalaRelational from any other existing framework. This
framework was created as the resulting frustration from using all the existing Scala database frameworks and being left
wanting more.

Finally, this tutorial is based on [this spec](https://github.com/outr/scalarelational/blob/master/mapper/src/test/scala/org/scalarelational/mapper/gettingstarted/GettingStartedSpec.scala)
and though we try to keep it updated, if you have any problems please refer to that as a working and compilable example.

## Getting Started

The first thing you need to do is add the H2 dependencies to your SBT project:

```scala
libraryDependencies += "org.scalarelational" % "scalarelational-h2" % "1.0.0"
```

or for mysql
```scala
libraryDependencies += "org.scalarelational" % "scalarelational-mysql" % "1.0.0"
```


## Setup your Database

The next thing you need is the database representation in Scala. This can map to an existing database or you can utilize
this to generate your database:

```scala
object GettingStartedDatastore extends H2Datastore(mode = H2Memory("getting_started")) {
  object suppliers extends Table("SUPPLIERS") {
    val name = column[String]("SUP_NAME", Unique)
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
    val id = column[Option[Int]]("SUP_ID", PrimaryKey, AutoIncrement)
  }

  object coffees extends Table("COFFEES") {
    val name = column[String]("COF_NAME", Unique)
    val supID = column[Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
    val id = column[Option[Int]]("COF_ID", PrimaryKey, AutoIncrement)
  }
}
```
or for mysql

```scala
object GettingStartedDatastore extends MysqlDatastore(
    MySQLConfig("localhost","databaseName", "user", "password"))
```

Our Datastore contains Tables and our Tables contain Columns. This is pretty straight-forward and easy to see how it
maps back to our database. Every column type must have a DataType associated with it. You don't see it referenced above
because all standard Scala types have pre-defined implicit conversions available in the Table class. If you need to use
a type that doesn't already have a converter or want to replace one it is trivial to do so and the compiler will complain
if a DataType isn't available pointing you in the right direction.

You may notice the striking similarity between this code and Slick's introductory tutorial. This was done purposefully
to allow better comparison of functionality between the two frameworks. An AutoIncrementing ID has been introduced into
both tables to better represent the standard development scenario. Rarely do you have external IDs to supply to your
database like [Slick represents](http://slick.typesafe.com/doc/3.0.0/gettingstarted.html#schema).

## Create your Database

Now that we have our database model created in Scala, we need to create it:

```scala
import GettingStartedDatastore._

session {
    create(suppliers, coffees)
}
```

All database requests must be within a session. The session manages the database connection on your behalf. Sessions
should not be confused with transactions. A session simply manages the connection whereas a transaction disables
auto-commit and automatically rolls back if an exception is thrown while executing. Don't worry too much about sessions
within sessions. ScalaRelational will ignore inner session creation and only maintain a connection for the outermost
session. Additionally, sessions are lazy and will only open a connection when one is needed so it is perfectly acceptable
to wrap blocks of code that may or may not access the database without being concerned about the performance implications.

You'll notice we imported GettingStartedDatastore._ in an effort to minimize the amount of code required here. We can explicitly
write it more verbosely like this:

```scala
GettingStartedDatastore.session {
    GettingStartedDatastore.create(GettingStartedDatastore.suppliers, GettingStartedDatastore.coffees)
}
```

However, that looks much uglier and far less SQLish so importing the datastore is generally suggested.

## Insert some Suppliers

Now that we've created our database we need to insert some data. We'll start with inserting some suppliers:

```scala
import suppliers._

var acmeId: Int = _
var superiorCoffeeId: Int = _
var theHighGroundId: Int = _

transaction {
    // Clean and type-safe inserts
    acmeId = insert(name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).result
    superiorCoffeeId = insert(name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).result

    // Short-hand when using values in order - we exclude the ID since it will be generated by the database
    theHighGroundId = insertInto(suppliers, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").result
}
```

Two insert styles are represented here. The first uses the column to wrap around a value to create a 'ColumnValue'. This
provides a very clean way of inserting the exact values you need and excluding values you want defaulted. This also
makes the code much easier to read.

The second representation uses **insertInto** and takes the 'Table' as the first argument followed by the values to be
inserted. **Note:** the items must be added in the same order as they are defined in your Table when you use **insertInto**.
This second method is provided mostly for people coming from Slick as it provides a very similar approach, but the first
method is preferred as there is no ambiguity of what value maps to what column and offers greater inserting flexibility.

The last thing to notice here is that instead of a **session** we are using a **transaction**. Because we are inserting
multiple suppliers we want to make sure everything inserts properly or rolls the entire transaction back. You can use
**transaction** instead of **session** as the first thing a **transaction** does is establish a **session** if one does
not already exist.

## Batch Insert some Coffees

Now that we have some suppliers, we need to add some coffees as well:

```scala
import coffees._

session {
    // Batch insert some coffees
    insert(name("Colombian"), supID(acmeId), price(7.99), sales(0), total(0)).
           and(name("French Roast"), supID(superiorCoffeeId), price(8.99), sales(0), total(0)).
           and(name("Espresso"), supID(theHighGroundId), price(9.99), sales(0), total(0)).
           and(name("Colombian Decaf"), supID(acmeId), price(8.99), sales(0), total(0)).
           and(name("French Roast Decaf"), supID(superiorCoffeeId), price(9.99), sales(0), total(0)).result
}
```

This is very similar to the previous insert method, except instead of calling **result** we're calling **and**. This
converts the insert into a batch insert and you gain the performance of being able to insert several records with one
insert statement.

## Query all the Coffees

We've finished inserting data now, so lets start querying some data back out. We'll start by simply iterating over the
coffees in the database and printing them out:

```scala
import coffees._

session {
    println("Coffees:")
    val query = select(*) from coffees
    query.result.foreach {
        case r => println(s"  ${r(name)}\t${r(supID)}\t${r(price)}\t${r(sales)}\t${r(total)}")
    }
}
```

It is worthwhile to notice that our query looks exactly like a SQL query, but it is Scala code using ScalaRelational's
DSL to provide this. Most of the time SQL queries in ScalaRelational look exactly the same as in SQL, but there are a
few more complex scenarios where this is not the case or even not preferred.

## Query all Coffees filtering and joining with Suppliers

Now that we've see a very basic query example, let's look a more advanced scenario:

```scala
session {
    println("Filtered Results:")
    val query = select(coffees.name, suppliers.name) from coffees innerJoin suppliers on coffees.supID.opt === suppliers.id where coffees.price < 9.0
    query.result.foreach {
        case r => println(s"  Coffee: ${r(coffees.name)}, Supplier: ${r(suppliers.name)}")
    }
}
```

You can see here that though this query looks very similar to a SQL query there are some slight differences. This is the
result of Scala's limitations for writing DSLs. For example, we must use "===" instead of "==".

ScalaRelational tries to retain type information where possible, and though loading data by columns is very clean, we
can actually extract a Tuple2[String, String] representing the coffee name and suppliers name in the following:

```scala
session {
    println("Filtered Results:")
    val query = select(coffees.name, suppliers.name) from coffees innerJoin suppliers on coffees.supID.opt === suppliers.id where coffees.price < 9.0
    query.result.foreach {
        case r => {
            val (coffeeName, supplierName) = r()
            println(s"  Coffee: $coffeeName, Supplier: $supplierName")
        }
    }
}
```

This is possible because the DSL supports explicit argument lists and retains type information all the way through to
the result. This allows us to do much more advanced things as we'll see later when discussing the Mapper sub-project.

## ORM Support

There has been a big shift recently away from ORMs since they tend to focus only on the most simplistic use-case for
dealing with your database and extremely complicate matters should you have a more complex usage scenario. In addition,
the performance penalties are pretty substantial and complicated when you must consider cascading relationship issues
when dealing with an ORM.

In previous iterations of this project we had an ORM module to allow mapping of SQL statements to case classes, but
recently this has been entirely removed. Though mapping classes to and from the database is still an extremely useful
thing to do, we have decided that a different approach was necessary. Where most (even anti-ORM) frameworks tend to
make the association between database and object is at that table level. However, this often isn't the most logical way
to deal with the data. Rather, it makes more sense to directly map queries, inserts, updates, merges, and deletes with
objects. This gives the added ability to have classes that represent many scenarios whether you're querying simple data
from one table, querying many columns joined across many tables, or dealing with calculated reports generated by a SQL
query. This is where the **Mapper** module comes in.

## Mapper

The mapper module provides functionality to auto or explicitly map when persisting and when querying. We must first add
another dependency to our build file:

```scala
libraryDependencies += "org.scalarelational" % "scalarelational-mapper" % "1.0.0"
```

## Create a Supplier case class

Though working directly with columns is easy in ScalaRelational, it's not nearly as easy as working with an ORM. The
mapper gives us the benefits of an ORM without the pain. First we need a class to map:

```scala
case class Supplier(name: String, street: String, city: String, state: String, zip: String, id: Option[Int] = None)
```

Though all of these fields are in the same order as the table, this is not required to be the case. Mapping takes place
based on the field name to the column name in the table so order doesn't matter.

## Persist a new Supplier

We've create a Supplier case class, but now we need to create an instance and persist it into the database:

```scala
import org.scalarelational.mapper._

session {
    val starbucks = Supplier("Starbucks", "123 Everywhere Rd.", "Lotsaplaces", "CA", "93966")
    val updated = suppliers.persist(starbucks).result
}
```

We first import the mapper package so we have access to the handy implicit conversions to allow persisting directly to a
table. Next we create a simple supplier instance, and finally we call **persist** on the **suppliers** table. It is worth
noting here that **updated** is a 'Supplier', but it has been updated to include the database-generated ID, so though
'starbucks.id' will be **None**, 'updated.id' will be **Some(4)** (since it is the fourth inserted supplier).

## Query a Supplier back

We've successfully inserted our Supplier instance, but now how do we query it back out? It's actually surprisingly easy:

```scala
import org.scalarelational.mapper._
import suppliers._

session {
    val query = select(*) from suppliers where name === "Starbucks"
    val starbucks = query.to[Supplier].result.head()
    println(s"Result: $starbucks")
}
```

That's all there is to it. The mapper will automatically match column names in the results to fields in the case class
provided. Every query can have its own class for convenience mapping.

## Query back Coffee and Supplier with join

Though what we've shown thus far for Mapper could be similarly accomplished in most other frameworks that have a concept
of an ORM, it is this example that shows a major point where we diverge:

```scala
session {
    val query = select(coffees.* ::: suppliers.*) from coffees innerJoin suppliers on(coffees.supID.opt === suppliers.id) where(coffees.name === "French Roast")
    val (frenchRoast, superior) = query.to[Coffee, Supplier](coffees, suppliers).result.head()
    println(s"Coffee: $frenchRoast, Supplier: $superior")
}
```

This is a very efficient SQL query to join the **coffees** table with the **suppliers** table and get back a single result
set. Using Mapper we are able to separate the columns relating to **coffees** from **suppliers** and map them directly
to our case classes.

## Polymorphic querying
It may be desired to represent a type hierarchy in a single table for better performance:

```scala
trait User {
  def name: String
  def id: Option[Int]
}

case class UserGuest(name: String, id: Option[Int] = None) extends User {
  val isGuest = true
}

case class UserAdmin(name: String, canDelete: Boolean, id: Option[Int] = None) extends User {
  val isGuest = false
}

object users extends Table("users") {
  val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
  val name = column[String]("name")
  val canDelete = column[Boolean]("canDelete", Polymorphic)
  val isGuest = column[Boolean]("isGuest")
}
```

Runtime reflection is used, which allows to insert a heterogeneous list of objects:

```scala
val insertUsers = Seq(
  UserGuest("guest"),
  UserAdmin("admin", true)
)

insertUsers.foreach(users.persist(_).result)
```

To query the table, you will need to evaluate the column which encodes the original type of the object, namely ``isGuest`` in this case. For more complex type hierarchies you may want to use an enumeration instead.

```scala
val query = select (*) from users

val x = query.asCase[User] { row =>
  if (row(users.isGuest)) classOf[UserGuest]
  else classOf[UserAdmin]
}

x.result.converted.toList // : List[User]
```

## More information

Hopefully this introduction was enough to show the benefits of using ScalaRelational for query building. For more examples
take a look at our ScalaTest specs in the source code. This was meant to be a very brief introduction to ScalaRelational,
but in no way is an exhaustive look at the features it provides.