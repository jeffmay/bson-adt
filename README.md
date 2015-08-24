Bson ADT
========
<a href="https://travis-ci.org/jeffmay/bson-adt">
<img src="https://travis-ci.org/jeffmay/bson-adt.svg" href="https://travis-ci.org/#" />
</a>
<a href='https://coveralls.io/github/jeffmay/bson-adt?branch=master'><img src='https://coveralls.io/repos/jeffmay/bson-adt/badge.svg?branch=master&service=github' alt='Coverage Status' /></a>
<table>
<tr>
  <th>bson-adt-mongo-async</th>
  <th>bson-adt-mongo</th>
  <th>bson-adt-casbah</th>
  <th>bson-adt-mongo2</th>
  <th>bson-adt-core</th>
  <th>bson-adt (legacy)</th>
</tr>
<tr>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt-mongo-async/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt-mongo-async/images/download.svg'></a></td>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt-mongo/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt-mongo/images/download.svg'></a></td>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt-casbah/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt-casbah/images/download.svg'></a></td>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt-mongo2/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt-mongo2/images/download.svg'></a></td>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt-core/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt-core/images/download.svg'></a></td>
  <td><a href='https://bintray.com/jeffmay/maven/bson-adt/_latestVersion'><img src='https://api.bintray.com/packages/jeffmay/maven/bson-adt/images/download.svg'></a></td>
</tr>
</table>

A closed generic algebraic data type for Bson serialization and deserialization in Scala

This library works a lot like the [play-json](https://www.playframework.com/documentation/2.3.x/ScalaJson) library,
but for BSON, the document storage format used by [MongoDB](http://docs.mongodb.org/manual/core/introduction/).

What's an Algebraic Data Type?
==============================

In short, an algebraic data type (ADT) is just a way of defining a closed set of values, kind of like an enumeration.
The only difference between an ADT and an enumeration is that an ADT can assign differently structured data and
operations based on the type of each value in the closed set.

In Bson, there are the following types (and more!):

* `BsonArray` - An array of `BsonValue`s
* `BsonBinary` - An array of `Byte`s
* `BsonBoolean` - Either `true` or `false`
* `BsonDate` - A joda [DateTime](http://www.joda.org/joda-time/apidocs/org/joda/time/DateTime.html)
* `BsonInt` - An `Int`
* `BsonLong` - A `Long`
* `BsonNull` - The `null` reference value
* `BsonDouble` - A floating point `Double`
* `BsonObject` - A key-value pair map of `String` property names to `BsonValue`s
* `BsonObjectId` - An `ObjectId` as [defined by Mongo](http://docs.mongodb.org/manual/reference/object-id/)
* `BsonRegex` - A regular expression (ie. `Regex`)
* `BsonString` - A `String` of characters
* `BsonUndefined` - (deprecated) The value of a reference that was not previously defined

Note there are other types in the [spec](http://bsonspec.org/spec.html), but this library currently only defines the
types mentioned above.

All of these values extend from `BsonValue`, and are two generic subclasses of value:

1. `BsonArray` and `BsonObject` extend from `BsonContainer`
2. All others are leaf values that extend from `BsonPrimitive`

Usage
=====

Creating BsonValues
-------------------

The purpose of this library is to define the common format of `BsonValue`s in Scala code, so that multiple drivers for
MongoDB (or some other BSON based database) can be used without needing to redefine the structure of the objects being
serialized.

All serializers are bound by type. This works out well in practice as case classes are a great way to define data
structures and they provide a named type handle.

To create a `BsonValue` without a serializer is easy.

```scala
import adt.bson.Bson

// For a BsonObject:
Bson.obj(
  "name" -> "Batman",
  "age" -> 42
)

// For a BsonArray:
Bson.arr(1, 2, 3)

// For a BsonPrimitive:
BsonString("Batman")
BsonInt(42)
// etc
```

Note that the `Bson.obj` and `Bson.arr` methods are able to tell at compile-time which values can be converted safely to
`BsonValue`s. This is done by having an implicit `BsonWrites` in scope for the type of value.

Bson Serializers / Deserializers
--------------------------------

In order to create a new `BsonWrites`, which can then be used by a library specific to your database driver to write
transfer the value.

```scala
case class User(name: String, age: Int)

implicit val writer = BsonWrites[User] { user =>
  Bson.obj(
    "name" -> user.name,
    "age" -> user.age
  )
}

// Then to use it
val batman = User("Batman", 42)
assert(Bson.toBson(batman) == writer.write(batman))
```

Note that this will of course compose:

```scala
Bson.obj(
  "user" -> batman
)
```

And some things compose implicitly for your special type `X`!

So long as you have an implicit `BsonWrites[X]`, `Map[String, X]` and and subclass of `Traversable[X]` are
automatically serializable through the power of implicits.

Great, so now we can write to the database. How do we read from it?

Why with a `BsonReads` of course!

```scala
implicit val reader = BsonReads[User] { bson =>
  val name = (bson \ "name").as[String]
  val age = (bson \ "age").as[Int]
  User(name, age)
}
```

Note that the following calls will throw an exception if the format is wrong. However, if you control the format of
what is in your database, then throwing an exception is probably reasonable.

In future versions, this may change to use a monad to collect any validation failures, and allow the implementer to
decide how they want to handle each case of failure.

And, of course, `BsonReads[X]` will implicitly allow reading a `Map[String, X]` and any subclass of `Traversable[X]`

And last, but not least, is the `BsonFormat`. It is both a `BsonWrites` and a `BsonReads` and the rules o

Casbah Syntax
=============

If you would like to be able to write your `BsonObject`s using Casbah, you can `import adt.bson.casbah.syntax._` to
enable interoperability and syntactic sugar.

```scala
// to convert from a BsonValue to a value that is safe for the Casbah driver
val bson: BsonValue = ???
collection.update(MongoDBObject("_id" -> ???), dbValue(bson))

// to convert a value from Casbah into a BsonValue
collection.findOne(MongoDBObject("_id" -> ???)).map(document => bsonValue(document))

// however, you might want this as a BsonObject, since all documents are going to be objects
collection.findOne(MongoDBObject("_id" -> ???)).map(document => bsonObject(document))

// there is also the following syntactic sugar
collection.findOne(MongoDBObject("_id" -> ???)).map(_.toBsonObject)

// extracting the right type from a `BsonValue` is pretty simple at this point
collection.findOne(MongoDBObject("_id" -> ???)).map(_.toBsonObject.as[T])

// if you want to fail silently on bad format, you could also flatten
collection.findOne(MongoDBObject("_id" -> ???)).flatMap(_.toBsonObject.asOpt[T])
```

If you want to extract specific values from Mongo without converting to `BsonValue` first, you can use the
`CasbahDBExtractors` that are provided by importing Casbah Syntax.

```scala
collection.findOne(MongoDBObject("_id" -> ???)) map {
  case DBList(list) =>
    list collect {
      case DBDate(date) => date
    }
  case _ => Seq.empty[DateTime]
}
```

While this gives you access to the underlying code for potential performance benefits or writing helper code,
it is probably best to just convert to `BsonValue` and operate on that. It will be safer and more reusable.


MongoDB Syntax
==============

If you are operating directly with MongoDB, you can use `import adt.bson.mongodb.syntax._` instead. It is just
like the Casbah Syntax (see above section) except that it returns the MongoDB driver version of `BasicDBObject`
and `BasicDBList`.

This mainly exists so that you can exclude the transitive dependency of Casbah.

ReactiveMongo Syntax
====================

Coming soon...
