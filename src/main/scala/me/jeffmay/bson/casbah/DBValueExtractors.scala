package me.jeffmay.bson.casbah

import com.mongodb.casbah.Imports._
import org.joda.time.{DateTime, DateTimeZone}

import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.matching.Regex

/**
 * Extracts a [[MongoDBList]] from a raw value, if possible.
 *
 * @note if attempting to extract a [[Traversable]], you should always put this extractor
 *       AFTER the [[DBObject]] extractor, as this is a little more lenient.
 *
 * [[BasicDBList]] is the only list representation in the underlying driver and it
 * extends from [[DBObject]]. The extractor will avoid checking this type.
 */
object DBList {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: MongoDBObject | _: BasicDBObject => false  // Don't match on DBObject
    case _: MongoDBList | _: BasicDBList | _: Traversable[_] | _: Array[_] => true
    case _ => false
  }

  def unapply(value: Any): Option[MongoDBList] = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: MongoDBObject | _: BasicDBObject => None  // Don't match on DBObject
    case x: MongoDBList => Some(x)
    case x: BasicDBList => Some(x)
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Traversable[_] => Some(MongoDBList.concat(x))
    case x: Array[_] =>       Some(MongoDBList.concat(x))
    case _ => None
  }
}

/**
 * Extracts a [[MongoDBObject]] from a raw value, if possible.
 *
 * @note this will also match a Map of String, but only if this extractor is used
 *       before the [[DBList]] extractor.
 *
 * [[BasicDBObject]] is the underlying representation for Mongo objects and
 * extends from [[DBObject]]. The extractor will exploit this inheritance.
 */
object DBObject {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: MongoDBList | _: BasicDBList => false
    case _: MongoDBObject | _: DBObject => true
    case x: Map[_, _] => x.keys forall (_.isInstanceOf[String])
    case _ => false
  }

  def unapply(value: Any): Option[MongoDBObject] = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: MongoDBList | _: BasicDBList => None
    case x: MongoDBObject => Some(x)
    case x: DBObject =>      Some(x)  // BasicDBObject extends from DBObject, so this will match both
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Map[_, _] if x.keys forall (_.isInstanceOf[String]) =>
      val fields = x.toSeq map { case (k, v) => (k.asInstanceOf[String], v) }
      Some(MongoDBObject(fields: _*))
    case _ => None
  }
}

/**
 * Extracts an [[Array]] of [[Byte]] from a raw value, if possible.
 */
object DBBinary {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean = value match {
    case null => false
    case _: org.bson.types.Binary => true
    case arr: Array[_] =>
      arr.length == 0 || arr(0).isInstanceOf[Byte]
    case _ => false
  }

  def unapply(value: Any): Option[Array[Byte]] = value match {
    case null => None
    case x: org.bson.types.Binary => Some(x.getData)
    case x: Array[_] if x.length == 0 || x(0).isInstanceOf[Byte] => Some(x.asInstanceOf[Array[Byte]])
    case _ => None
  }
}

/**
 * Extracts a [[java.util.regex.Pattern]] from a raw value, if possible.
 */
object DBRegex {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean = value match {
    case null => false
    case _: java.util.regex.Pattern | _: Regex => true
    case _ => false
  }

  def unapply(value: Any): Option[Regex] = value match {
    case null => None
    case x: java.util.regex.Pattern => Some(x.pattern.r)
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Regex => Some(x)
    case _ => None
  }
}

/**
 * Extracts a [[org.joda.time.DateTime]] from a raw value, if possible.
 */
object DBDate {

  /**
   * Returns true if the value can be extracted.
   */
  def matches(value: Any): Boolean = value match {
    case null => false
    case _: java.util.Date | _: DateTime => true
    case _ => false
  }

  def unapply(value: Any): Option[DateTime] = value match {
    case null => None
    case x: java.util.Date => Some(new DateTime(x.getTime, DateTimeZone.UTC))
    case x: DateTime => Some(x)
    case _ => None
  }
}

/**
 * A wrapper for any value that can be safely stored to Mongo using the Java driver used by Casbah.
 *
 * @note This is only used to guarantee safety when storing the value.
 *       It does not guarantee safety of the value having the right type for your field.
 */
class DBValue private[casbah](val value: AnyRef) extends AnyVal

object DBValue {

  /**
   * Extract a DBValue from the given value or throw an exception.
   *
   * @note while this is an unusual pattern for apply / unapply methods,
   *       it fits with the way [[DBObject]] works.
   *
   * @param value the value to convert to a safe DBValue
   */
  def apply(value: Any): DBValue = {
    unapply(value) getOrElse {
      val className = Option(value).map(_.getClass.getName) getOrElse "Null"
      throw new IllegalArgumentException(
        s"$value of type $className cannot be directly converted to a BSON value in Mongo"
      )
    }
  }

  /**
   * Returns true if the value can be extracted.
   */
  @tailrec
  def matches(value: Any): Boolean = {
    DBObject.matches(value) || DBList.matches(value) || DBDate.matches(value) || DBRegex.matches(value) || (
      value match {
        case Some(x) => matches(x) // unwrap all options to find out
        case null | None
             | _: String | _: ObjectId
             | _: java.lang.Boolean | _: java.lang.Integer | _: java.lang.Long
             | _: java.lang.Double | _: java.lang.Float => true
        case _ => false
      }
    )
  }
  
  /**
   * Extracts a known type of [[DBValue]] from a given value of any type.
   *
   * This will filter out any unknown types of values and provide a type-safe wrapper for the return value.
   */
  @tailrec
  def unapply(value: Any): Option[DBValue] = value match {
    case None | null => Some(new DBValue(null))  // null is an acceptable value in Mongo
    case Some(x) => unapply(x)
    case x: String => Some(new DBValue(x))
    case x: ObjectId => Some(new DBValue(x))
    case x: java.lang.Boolean => Some(new DBValue(x))
    case x: java.lang.Integer => Some(new DBValue(x))
    case x: java.lang.Long => Some(new DBValue(x))
    case x: java.lang.Double => Some(new DBValue(x))
    case x: java.lang.Float => Some(new DBValue(x))
    case DBBinary(bin) => Some(new DBValue(bin))
    // Skipping empty objects, since these should probably fall back to an array
    case DBObject(dbo) if dbo.nonEmpty => Some(new DBValue(dbo))
    case DBList(lst) => Some(new DBValue(lst))
    case DBDate(date) => Some(new DBValue(date.toDate))  // Mongo only stores java.util.Date
    case DBRegex(pattern) => Some(new DBValue(pattern.pattern))  // Mongo only stores java.util.regex.Pattern
    case _ => None
  }

}