package adt.bson

import org.bson.BSON
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.util.Try
import scala.util.matching.Regex

/**
 * Generic Bson value.
 *
 * This is a closed algebraic data type for all values that can be stored in Mongo.
 *
 * See {@link http://bsonspec.org/spec.html}
 *
 * (Currently) Unsupported Types:
 * Undefined                \x06 / 6    (deprecated)
 * DBPointer                \x0C / 12   (deprecated)
 * JavaScript               \x0D / 13   (javascript in the database? are you sure you want this noose?)
 * Symbol                   \x0E / 14   (deprecated)
 * JavaScript (with scope)  \x0F / 15   (javascript in the database? with scope? really?)
 * Timestamp                \x11 / 17   (used for internal increment values)
 * Min key                  \xFF / 255  (used internally)
 * Max key                  \x7F / 127  (used internally)
 */
sealed trait BsonValue {
  type $type
  def $type: Int
  def value: $type

  /**
   * Tries to convert the value into a [[T]], throwing an exception if it can't.
   * An implicit [[BsonReads]] of [[T]] must be defined.
   */
  def as[T](implicit reader: BsonReads[T]): T = reader reads this

  /**
   * Tries to convert the value into a [[T]]. An implicit [[BsonReads]] of [[T]] must be defined.
   * Any error is mapped to None
   *
   * @return Some[T] if it succeeds, None if it fails.
   */
  def asOpt[T](implicit reader: BsonReads[T]): Option[T] = Try(reader reads this).toOption.filter {
    case BsonUndefined() => false
    case _ => true
  }

  /**
   * Return the property corresponding to the fieldName, supposing we have a [[BsonObject]].
   *
   * @param fieldName the name of the property to lookup
   * @return the resulting [[BsonValue]].
   *         If the current node is not a [[BsonObject]] or doesn't have the property,
   *         a [[BsonUndefined]] will be returned.
   */
  def \(fieldName: String): BsonValue = BsonUndefined(s"'$fieldName' is undefined on object: $this")

  /**
   * Whether {{{value == null}}} would be true in JavaScript / Mongo.
   */
  def equalsNull: Boolean = false

  /**
   * Strips out any `null` values from arrays and `null` fields from objects.
   *
   * @note This does not remove empty arrays or objects.
   */
  def pruned: BsonValue = this

}

/**
 * Any [[BsonValue]] that can exist inside of another [[BsonContainer]], but has no children itself.
 *
 * @note this
 */
sealed trait BsonPrimitive extends BsonValue

/**
 * Any [[BsonValue]] that can potentially contain [[BsonPrimitive]] children.
 */
sealed trait BsonContainer extends BsonValue {

  def children: Iterable[BsonValue]
}

case class BsonBoolean(value: Boolean) extends BsonPrimitive {
  override type $type = Boolean
  override def $type: Int = BSON.BOOLEAN
}

case class BsonNumber(value: Double) extends BsonPrimitive {
  override type $type = Double
  override def $type: Int = BSON.NUMBER
}

case class BsonInt(value: Int) extends BsonPrimitive {
  override type $type = Int
  override def $type: Int = BSON.NUMBER_INT
}

case class BsonLong(value: Long) extends BsonPrimitive {
  override type $type = Long
  override def $type: Int = BSON.NUMBER_LONG
}

case class BsonString(value: String) extends BsonPrimitive {
  override type $type = String
  override def $type: Int = BSON.STRING
}

case class BsonBinary(value: Array[Byte]) extends BsonPrimitive with Proxy {
  override type $type = Array[Byte]
  override def $type: Int = BSON.BINARY

  val utf8: String = new String(value, "UTF-8")

  @inline final override def self: Any = utf8
}

/**
 * A Regular Expression value.
 *
 * @note this extends [[Proxy]] to allow the underlying immutable pattern String
 *       determine equals and hashCode rather than [[Regex]], which does not define
 *       equality based on the value of the compiled pattern
 *
 * @param value the regex value
 */
case class BsonRegex(value: Regex) extends BsonPrimitive with Proxy {
  override type $type = Regex
  override def $type: Int = BSON.REGEX

  val pattern: String = value.pattern.pattern()

  @inline final override def self: Any = pattern
}

case class BsonObjectId(value: ObjectId) extends BsonPrimitive {
  override type $type = ObjectId
  override def $type: Int = BSON.OID
}

case class BsonDate(value: DateTime) extends BsonPrimitive {
  override type $type = DateTime
  override def $type: Int = BSON.DATE
}

case class BsonArray(value: Seq[BsonValue] = Nil) extends BsonContainer {
  override type $type = Seq[BsonValue]
  override def $type: Int = BSON.ARRAY

  @inline final override def children: Iterable[BsonValue] = value

  override def pruned: BsonArray = BsonArray(
    value filterNot (_.equalsNull) map (_.pruned)
  )
}

case class BsonObject(value: Map[String, BsonValue] = Map.empty) extends BsonContainer {
  override type $type = Map[String, BsonValue]
  override def $type: Int = BSON.OBJECT

  override def children: Iterable[BsonValue] = value.values

  override def \(fieldName: String): BsonValue = {
    value.getOrElse(fieldName, super.\(fieldName))
  }

  override def pruned: BsonObject = BsonObject(
    value filterNot (_._2.equalsNull) mapValues (_.pruned)
  )
}

/**
 * Represents a Bson `null` value.
 */
case object BsonNull extends BsonPrimitive {
  override def value: Null = null  // throw new NullPointerException?
  override type $type = Null
  override def $type: Int = BSON.NULL

  override def asOpt[T](implicit reader: BsonReads[T]): Option[T] = None

  override def equalsNull: Boolean = true
}

/**
 * Represent a missing Bson value.
 */
class BsonUndefined(err: => String) extends BsonValue {
  override type $type = Nothing
  override def value: $type = throw new NoSuchElementException("Cannot read value of undefined")
  override def $type: Int = BSON.UNDEFINED
  def error = err
  override def toString = "BsonUndefined(" + err + ")"

  override def asOpt[T](implicit reader: BsonReads[T]): Option[T] = None

  override def equalsNull: Boolean = true
}

object BsonUndefined {
  def apply(err: => String): BsonUndefined = new BsonUndefined(err)
  def unapply(value: Object): Boolean = value.isInstanceOf[BsonUndefined]
}
