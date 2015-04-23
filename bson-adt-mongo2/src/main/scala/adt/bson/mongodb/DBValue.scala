package adt.bson.mongodb

import adt.bson.BsonValue

/**
 * A wrapper for any value that can be safely stored to Mongo using the driver in scope.
 *
 * @note This is only used to guarantee safety when storing the value.
 *       It does not guarantee safety of the value having the right type for your field.
 *
 *       The first iteration of this was a type class, but the end result was just duplicating
 *       the whole intent of [[BsonValue]] and [[adt.bson.BsonWrites]].
 *
 *       It is probably best to operate on [[BsonValue]] in helper methods and only convert
 *       to a [[DBValue]] for purposes of knowing generically at compile-time that the value has
 *       been converted to a value that is safe for Mongo and doesn't need to be converted again.
 *       It is slightly safer than the alternative of holding the common base type of [[AnyRef]].
 */
class DBValue private[adt](val value: AnyRef) // extends AnyVal
/*
Cannot extend AnyVal in Scala 2.10, because DBValueCompanion extends DBCompanion[Any, DBValue],
and this breaks compilation...

[ERROR] bridge generated for member method apply: (value: Any)adt.bson.DBValue in object DBValue
[ERROR] which overrides method apply: (arguments: A)T in trait DBBuilder
[ERROR] clashes with definition of the member itself;
[ERROR] both have erased type (arguments: Object)Object
[ERROR]     override def apply(value: Any): DBValue

This is supposedly fixed in Scala 2.11.0
 */

/**
 * A pattern matching helper that will extract the expected type of value from a valid returned
 * by the Java MongoDB driver.
 */
trait DBExtractor[T] {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean

  /**
   * Extracts the value if it matches, otherwise None.
   *
   * @param value the value from which to extract an instance of [[T]]
   * @return Some value derived instance or None
   */
  def unapply(value: Any): Option[T]
}

/**
 * A [[DBExtractor]] pattern matching helper that can also build instances of the type from tuples.
 */
trait DBCompanion[-A, T] extends (A => T) with DBExtractor[T] {

  /**
   * Builds an instance of [[T]] from the given arguments.
   *
   * @param arguments the arguments required to build an instance of [[T]]
   * @return the finalized / constructed value
   */
  override def apply(arguments: A): T
}

/**
 * A [[DBCompanion]] for extracting [[DBValue]] instances and
 */
trait DBValueCompanion extends DBCompanion[Any, DBValue] {

  /**
   * Extract a DBValue from the given value or throw an exception.
   *
   * @note while this is an unusual pattern for apply / unapply methods,
   *       it fits with the way [[com.mongodb.DBObject]] works.
   *
   * @param value the value to convert to a safe DBValue
   * @throws IllegalArgumentException if the type of value given is not supported by MongoDB's default serializers
   */
  @throws[IllegalArgumentException]("if the type of value given is not supported by MongoDB's default serializers")
  override def apply(value: Any): DBValue
}
