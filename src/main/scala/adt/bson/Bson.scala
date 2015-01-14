package adt.bson

import scala.language.implicitConversions

/**
 * Helper functions to handle [[BsonValue]]s.
 */
object Bson {

  /**
   * Magnet trait that allows Simplified Bson syntax.
   *
   * Example:
   * {{{
   *   BsonObject(Map(
   *      "key1", BsonString("value"),
   *      "key2" -> BsonNumber(123),
   *      "key3" -> BsonObject(Map("key31" -> BsonString("value31")))
   *   )) == Bson.obj( "key1" -> "value", "key2" -> 123, "key3" -> obj("key31" -> "value31"))
   *
   *   BsonArray(BsonString("value"), BsonNumber(123), BsonBoolean(true)) == Bson.arr( "value", 123, true )
   * }}}
   *
   * There is an implicit conversion from any Type with a [[BsonWrites]] to [[BsonValueWrapper]]
   * which is an empty trait that shouldn't end into unexpected implicit conversions.
   *
   * @note Due to [[BsonValueWrapper]] extending [[NotNull]] `null` or `None` will end in a compiler error;
   *       Use [[BsonNull]] instead.
   */
  sealed trait BsonValueWrapper extends NotNull

  private case class BsonValueWrapperImpl(bson: BsonValue) extends BsonValueWrapper

  implicit def toBsonValueWrapper[T](fieldValue: T)(implicit writer: BsonWrites[T]): BsonValueWrapper =
    BsonValueWrapperImpl(writer writes fieldValue)

  def obj(fields: (String, BsonValueWrapper)*): BsonObject =
    BsonObject(fields.collect {
      case (fieldName, BsonValueWrapperImpl(fieldValue)) => (fieldName, fieldValue)
    }.toMap)

  def arr(values: BsonValueWrapper*): BsonArray =
    BsonArray(values.map(_.asInstanceOf[BsonValueWrapperImpl].bson))

  // TODO: Improve
  def prettyPrint(bson: BsonValue): String = bson.toString

  /**
   * Provided a [[BsonWrites]] for its type, convert any object into a [[BsonValue]].
   *
   * @param value Value to convert to a [[BsonValue]]
   */
  def toBson[A: BsonWrites](value: A): BsonValue = BsonWrites.of[A] writes value

  /**
   * Provided a [[BsonWrites]] for its type, convert any traversable object into a [[BsonArray]].
   *
   * @param values [[Traversable]] of values to convert to [[BsonArray]]
   */
  def toBsonArray[A: BsonWrites](values: Traversable[A]): BsonArray = {
    BsonWrites.traversableWrites[A].writes(values).asInstanceOf[BsonArray]
  }

  /**
   * Provided a [[BsonObjectWrites]] for its type, convert any object into a [[BsonObject]].
   *
   * @param value Object to convert to a [[BsonObject]]
   */
  def toBsonObject[A: BsonObjectWrites](value: A): BsonObject = {
    BsonObjectWrites.of[A] writes value
  }

  /**
   * Provided a [[BsonReads]] for its type, convert a [[BsonValue]] to any type.
   *
   * @param bson Bson value to transform as an instance of T.
   */
  def fromBson[A: BsonReads](bson: BsonValue): A = BsonReads.of[A] reads bson

}
