# ScalaRelational: Better SQL in Scala

[![Build Status](https://travis-ci.org/outr/scalarelational.svg?branch=master)](https://travis-ci.org/outr/scalarelational)
[![Stories in Ready](https://badge.waffle.io/outr/scalarelational.png?label=ready&title=Ready)](https://waffle.io/outr/scalarelational)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/scalarelational)

**ScalaRelational** was created after evaluating all the existing database frameworks for working with SQL and finding them
both frustrating to work with and overly complex. Most database frameworks attempt to abstract SQL with the goal of
making working with your database easier, but this comes at the cost of inefficient queries, missing features, and
confusing code mapping. The goal of ScalaRelational is to provide a one-to-one representation of the SQL language in
Scala and provide performance improvements to working with the database.

Currently, only H2 database is supported, but other database dialects can be easily added as needed.

It is worth mentioning that much of the functionality of this framework was inspired by other database frameworks like
Slick.

## Getting Started

The first thing you need to do is add the dependencies to your SBT project:

```scala
libraryDependencies += "org.scalarelational" % "scalarelational-h2" % "1.0.0"
```

## Setup your Database

The next thing you need is the database representation in Scala. This can map to an existing database or you can utilize
this to generate your database:

```scala
object ExampleDatastore extends H2Datastore(mode = H2Memory("example")) {
  object suppliers extends Table("SUPPLIERS") {
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
  }

  object coffees extends Table("COFFEES") {
    val name = column[String]("COF_NAME", PrimaryKey)
    val supID = column[Int]("SUP_ID", new ForeignKey(ExampleDatastore.suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
  }
}
```

Our Datastore contains Tables and our Tables contain Columns. This is pretty straight-forward and easy to see how it
maps back to our database. Every column type must have an associated DataType associated with it. You don't see it
referenced above because all standard Scala types have pre-defined implicit conversions available in the Table class. If
you need to use a type that doesn't already have a converter or want to replace one it is trivial to do so.

You may notice the striking similarity between this code and Slick's introductory tutorial. This was done purposefully
to allow better comparison of functionality between the two frameworks.

## Create your Database

Now that we have our database model created in Scala, we need to create it:

```scala
import ExampleDatastore._

session {
    create(suppliers, coffees)
}
```

All database requests must be within a session. The session manages the database connection on your behalf. Sessions
should not be confused with transactions. A session simply manages the connection whereas a transaction disables
auto-commit and automatically rolls back if an exception is thrown while executing.

You'll notice we imported ExampleDatastore._ in an effort to minimize the amount of code required here. We can explicitly
write it more verbosely like this:

```scala
ExampleDatastore.session {
    ExampleDatastore.create(ExampleDatastore.suppliers, ExampleDatastore.coffees)
}
```

However, that looks much uglier so importing the datastore is generally suggested.

## Insert some Suppliers

Now that we've created our database we need to insert some data. We'll start with inserting some suppliers:

```scala
import suppliers._

session {
    // Clean and type-safe inserts
    insert(id(101), name("Acme, Inc."), street("99 Market Street"), city("Groundsville"), state("CA"), zip("95199")).result
    insert(id(49), name("Superior Coffee"), street("1 Party Place"), city("Mendocino"), state("CA"), zip("95460")).result
    
    // Short-hand when using values in order
    insertInto(suppliers, 150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").result
}
```

Two insert styles are represented here. The first uses the column to wrap around a value to create a ColumnValue. This
provides a very clean way of inserting the exact values you need and excluding values you want defaulted. This also
makes the code much easier to read.

The second representation uses **insertInto** and takes the Table as the first argument followed by the values to be
inserted. **Note:** the items must be added in the same order as they are defined in your Table when you use **insertInto**.

## Batch Insert some Coffees

Now that we have some suppliers, we need to add some coffees as well:

```scala
import coffees._

session {
    // Batch insert some coffees
    insert(name("Colombian"), supID(101), price(7.99), sales(0), total(0)).
       add(name("French Roast"), supID(49), price(8.99), sales(0), total(0)).
       add(name("Espresso"), supID(150), price(9.99), sales(0), total(0)).
       add(name("Colombian Decaf"), supID(101), price(8.99), sales(0), total(0)).
       add(name("French Roast Decaf"), supID(49), price(9.99), sales(0), total(0)).result
}
```

This is very similar to the previous insert method, except instead of calling **result** we're calling **add**. This
converts the insert into a batch insert and you gain the performance of being able to insert several records with one
insert statement.

## Query all the Coffees

We've finished inserting data now, so lets start querying some data back out. We'll start by simply iterating over the
coffees in the database and printing them out:

```scala
import coffees._

session {
    println("Coffees:")
    (select(*) from coffees).result.foreach {
        case r => println(s"  ${r(name)}\t${r(supID)}\t${r(price)}\t${r(sales)}\t${r(total)}")
    }
}
```

It is worthwhile to notice that our query looks exactly like a SQL query, but it is Scala code using ScalaRelational's
DSL to provide this. Most of the time SQL queries in ScalaRelational look exactly the same as in SQL, but there are a
few more complex scenarios where this is not the case.

## Query all Coffees filtering and joining with Suppliers

Now that we've see a very basic query example, let's look a more advanced scenario:

```scala
session {
    println("Filtered Results:")
    (select(coffees.name, suppliers.name) from coffees innerJoin suppliers on coffees.supID === suppliers.id where coffees.price < 9.0).result.foreach {
        case r => println(s"  Coffee: ${r(coffees.name)}, Supplier: ${r(suppliers.name)}")
    }
}
```

You can see here that though this query looks very similar to a SQL query there are some slight differences. This is the
result of Scala's limitations for writing DSLs. For example, we must use "===" instead of "==".

## ORM Support

In previous iterations of this project we had an ORM module to allow mapping of SQL statements to case classes, but
recently this has been entirely removed. We are now in the process of creating a much lighter-weight on-demand mapping
system that removes the burden of an ORM while keeping the benefits of object-oriented representation of data. This will
provide ad-hoc mapping of results into one or more case classes to allow each query to couple with a class or classes
instead of hard link between Table and Object. This tutorial will be updated with information on usage when it is ready.

## More information

Hopefully this introduction was enough to show the benefits of using ScalaRelational for query building. For more examples
take a look at our tests in the source code.