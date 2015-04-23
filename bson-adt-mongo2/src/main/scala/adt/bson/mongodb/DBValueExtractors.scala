package adt.bson.mongodb

import adt.bson.mongodb
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.annotation.tailrec
import scala.language.reflectiveCalls
import scala.util.matching.Regex

/**
 * Extend this to add to scope the bson database friendly value extractors (see [[DBExtractor]]).
 */
trait DBValueExtractors {

  type DBListType <: AnyRef
  type DBObjectType <: AnyRef
  type DBBinaryType = Array[Byte]
  type DBRegexType = Regex
  type DBDateType = DateTime

  val DBList: DBExtractor[DBListType]
  val DBObject: DBExtractor[DBObjectType]
  val DBBinary: DBExtractor[DBBinaryType]
  val DBRegex: DBExtractor[DBRegexType]
  val DBDate: DBExtractor[DBDateType]
  val DBValue: DBCompanion[Any, mongodb.DBValue]
}

trait DefaultDBValueExtractor {
  self: DBValueExtractors =>

  protected def isEmpty(dbo: DBObjectType): Boolean

  object DBValue extends DBValueCompanion {

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
    override def apply(value: Any): mongodb.DBValue = {
      this.unapply(value) getOrElse {
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
    override def matches(value: Any): Boolean = {
      self.DBObject.matches(value) || self.DBList.matches(value) || self.DBDate.matches(value) || self.DBRegex.matches(value) || (
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
    override def unapply(value: Any): Option[mongodb.DBValue] = value match {
      case None | null => Some(new DBValue(null))  // null is an acceptable value in Mongo
      case Some(x) => unapply(x)
      case x: String => Some(new DBValue(x))
      case x: ObjectId => Some(new DBValue(x))
      case x: java.lang.Boolean => Some(new DBValue(x))
      case x: java.lang.Integer => Some(new DBValue(x))
      case x: java.lang.Long => Some(new DBValue(x))
      case x: java.lang.Double => Some(new DBValue(x))
      case x: java.lang.Float => Some(new DBValue(x))
      case self.DBBinary(bin) => Some(new DBValue(bin))
      // Skipping empty objects, since these should probably fall back to an array
      case self.DBObject(dbo) if !isEmpty(dbo) => Some(new DBValue(dbo))
      case self.DBList(lst) => Some(new DBValue(lst))
      case self.DBDate(date) => Some(new DBValue(date.toDate))  // Mongo only stores java.util.Date
      case self.DBRegex(pattern) => Some(new DBValue(pattern.pattern))  // Mongo only stores java.util.regex.Pattern
      case _ => None
    }

  }
}
