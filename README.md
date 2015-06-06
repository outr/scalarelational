# ScalaRelational: Better SQL in Scala

[![Build Status](https://travis-ci.org/outr/scalarelational.svg?branch=master)](https://travis-ci.org/outr/scalarelational)
[![Stories in Ready](https://badge.waffle.io/outr/scalarelational.png?label=ready&title=Ready)](https://waffle.io/outr/scalarelational)

**ScalaRelational** was created after evaluating all the existing database frameworks for working with SQL and finding them
both frustrating to work with and overly complex. Most database frameworks attempt to abstract SQL with the goal of
making working with your database easier, but this comes at the cost of inefficient queries, missing features, and
confusing code mapping. The goal of ScalaRelational is to provide a one-to-one representation of the SQL language in
Scala and provide performance improvements to working with the database.

Currently, only H2 database is supported, but other database dialects can be easily added as needed.

It is worth mentioning that much of the functionality of this framework was inspired by other database frameworks like
Slick.