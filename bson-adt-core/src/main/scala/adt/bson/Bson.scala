package adt.bson

import org.bson.types.ObjectId
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}

import scala.language.implicitConversions
import scala.util.control.Breaks._
import scala.util.control.ControlThrowable

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
   *      "key2" -> BsonDouble(123),
   *      "key3" -> BsonObject(Map("key31" -> BsonString("value31")))
   *   )) == Bson.obj( "key1" -> "value", "key2" -> 123, "key3" -> obj("key31" -> "value31"))
   *
   *   BsonArray(BsonString("value"), BsonDouble(123), BsonBoolean(true)) == Bson.arr( "value", 123, true )
   * }}}
   *
   * There is an implicit conversion from any Type with a [[BsonWrites]] to [[BsonValueWrapper]]
   * which is an empty trait that shouldn't end into unexpected implicit conversions.
   *
   * @note Due to [[BsonValueWrapper]] extending [[NotNull]] `null` or `None` will end in a compiler error;
   *       Use [[BsonNull]] instead.
   */
  sealed trait BsonValueWrapper

  private case class BsonValueWrapperImpl(bson: BsonValue) extends BsonValueWrapper

  implicit def toBsonValueWrapper[T](fieldValue: T)(implicit writer: BsonWrites[T]): BsonValueWrapper =
    BsonValueWrapperImpl(writer writes fieldValue)

  def obj(fields: (String, BsonValueWrapper)*): BsonObject =
    BsonObject(fields.collect {
      case (fieldName, BsonValueWrapperImpl(fieldValue)) => (fieldName, fieldValue)
    }.toMap)

  def arr(values: BsonValueWrapper*): BsonArray =
    BsonArray(values.map(_.asInstanceOf[BsonValueWrapperImpl].bson))

  /**
   * TODO: Include double precision and large number formatting
   *
   * The format for printing Bson to a String.
   *
   * @param indentation the indentation config for determining how to serialize the bson
   * @param inlineWithin Attempt to inline nested values to keep them within the given number of characters.
   *                     If individual values are too long, this will not wrap them. None means to always
   *                     wrap and never attempt to inline.
   * @param dateTimeFormat the formatter to use for [[org.joda.time.DateTime]]s
   * @param binaryFormat the format to use for serializing an [[Array]] of [[Byte]]
   * @param oidFormat the format to use for serializing an [[ObjectId]]
   */
  case class StringifyFormat(
    indentation: Indentation = Indentation.pretty,
    inlineWithin: Option[Int] = Some(80),
    dateTimeFormat: DateTimeFormatter = ISODateTimeFormat.dateTime,
    binaryFormat: Array[Byte] => String = new String(_, "UTF-8"),
    oidFormat: ObjectId => String = oid => "ObjectId(\"" + oid.toString + "\")"
  ) {

    def inline: StringifyFormat = {
      if (indentation.tabSize == 0 && !indentation.newlines && inlineWithin.isEmpty) this
      else copy(indentation = indentation.copy(tabSize = 0, newlines = false), inlineWithin = None)
    }
  }
  object StringifyFormat {

    implicit val pretty = StringifyFormat()
    val inline = StringifyFormat(
      indentation = Indentation.minimal,
      inlineWithin = None
    )
  }

  /**
   * The indentation configuration for the [[StringifyFormat]].
   *
   * @param tabSize how many spaces to indent after new lines (only applies if newlines is true)
   * @param afterComma append a space after each comma
   * @param beforeColon append a space before each colon
   * @param afterColon append a space after each colon
   * @param padArray append a space at the start and end of each array (except empty arrays)
   * @param padObject append a space at the start and end of each object (except empty objects)
   * @param newlines append new line characters between each item or field in arrays and objects
   */
  case class Indentation(
    tabSize: Int,
    afterComma: Boolean,
    beforeColon: Boolean,
    afterColon: Boolean,
    padArray: Boolean,
    padObject: Boolean,
    newlines: Boolean)
  object Indentation {

    val pretty = Indentation(
      tabSize = 4,
      afterComma = true,
      beforeColon = true,
      afterColon = true,
      padArray = true,
      padObject = true,
      newlines =true
    )

    val minimal = Indentation(
      tabSize = 0,
      afterComma = false,
      beforeColon = false,
      afterColon = false,
      padArray = false,
      padObject = false,
      newlines =false
    )
  }

  /**
   * @see [[printInternal]]
   */
  private def stringifyInternal(value: BsonValue, maxTotalLength: Int)(implicit format: StringifyFormat): String = {
    val buffer = new StringBuffer(maxTotalLength)
    printInternal(value, buffer, 0, maxTotalLength)
    buffer.toString
  }

  /**
   * @param value the [[BsonValue]] to serialize as a string
   * @param buffer the buffer to print to
   * @param depth the current depth of the tree
   * @param maxTotalLength if greater than 0, throw exception if this length is exceeded
   * @param key the current key of the value being serialized (defaults to empty for when printing outside of an object)
   * @param format the formatting rules to use
   * @return the Bson formatted as a String
   * @throws ControlThrowable if maxLength is greater than 0 and exceeded
   */
  private def printInternal(
    value: BsonValue,
    buffer: StringBuffer,
    depth: Int,
    maxTotalLength: Int,
    key: String = "")(implicit format: StringifyFormat): Unit = {
    // these should be small enough to inline
    @inline def appendIf(cond: Boolean, c: Char): Unit = {
      if (cond) {
        buffer.append(c)
      }
    }
    @inline def appendN(n: Int, c: Char): Unit = {
      var i = 0
      while (i < n) {
        buffer.append(c)
        i += 1
      }
    }
    @inline def breakIfMaxLengthExceeded(): Unit = {
      if (maxTotalLength > 0 && buffer.length() > maxTotalLength) {
        break()
      }
    }
    @inline def newlineAndIndent(d: Int): Unit = {
      if (format.indentation.newlines) {
        buffer.append('\n')
        appendN(format.indentation.tabSize * d, ' ')
      }
    }
    // print this value and all children recursively
    value match {
      // append all flat values
      case BsonString(x) =>
        buffer.append('"')
        buffer.append(x)
        buffer.append('"')
      case BsonBoolean(x) =>
        buffer.append(x)
      case BsonInt(x) =>
        buffer.append(x)
      case BsonLong(x) =>
        buffer.append(x)
      case BsonDouble(x) =>
        buffer.append(x)
      case BsonObjectId(oid) =>
        buffer.append(format.oidFormat(oid))
      case BsonDate(datetime) =>
        buffer.append(datetime.toString(format.dateTimeFormat))
      case BsonNull =>
        buffer.append("null")
      case regex: BsonRegex =>
        buffer.append('/')
        buffer.append(regex.pattern)
        buffer.append('/')
      case BsonBinary(bytes) =>
        buffer.append('"')
        buffer.append(format.binaryFormat(bytes))
        buffer.append('"')
      case BsonUndefined() =>
        buffer.append("undefined")

      // handle collections
      case col: BsonContainer =>
        // inline small collections if they fit on one line
        var inlineBson: String = null
        if (format.indentation.tabSize > 0 && format.inlineWithin.isDefined) {
          val keyLength: Int =
            depth * format.indentation.tabSize +  // indentation to key
            key.length + 2 + // key with quotes around it
            ((if (format.indentation.beforeColon) 1 else 0) + 1 + (if (format.indentation.afterColon) 1 else 0))
          breakable {
            val result = stringifyInternal(
              value,
              format.inlineWithin.get - keyLength
            )(format.inline)
            assert(keyLength + result.length <= format.inlineWithin.get)
            inlineBson = result
          }
        }
        // either append the inline version or chop down
        if (inlineBson ne null) {
          buffer.append(inlineBson)
        }
        else {
          // chop down long lines
          col match {

            case BsonArray(values) =>
              buffer.append('[')
              appendIf(values.nonEmpty && !format.indentation.newlines && format.indentation.padArray, ' ')
              for (value <- values) {
                newlineAndIndent(depth + 1)
                printInternal(value, buffer, depth + 1, maxTotalLength)
                buffer.append(',')
                appendIf(format.indentation.afterComma, ' ')
              }
              // remove the final comma and apply proper indentation
              if (values.nonEmpty) {
                buffer.setLength(buffer.length() - (1 + (if (format.indentation.afterComma) 1 else 0)))
                newlineAndIndent(depth)
                appendIf(!format.indentation.newlines && format.indentation.padArray, ' ')
              }
              buffer.append(']')

            case BsonObject(fields) =>
              buffer.append('{')
              appendIf(fields.nonEmpty && !format.indentation.newlines && format.indentation.padObject, ' ')
              for ((key, value) <- fields) {
                // add indentation
                newlineAndIndent(depth + 1)
                buffer.append('"')
                buffer.append(key)
                buffer.append('"')
                appendIf(format.indentation.beforeColon, ' ')
                buffer.append(':')
                appendIf(format.indentation.afterColon, ' ')
                printInternal(value, buffer, depth + 1, maxTotalLength)
                buffer.append(',')
                appendIf(format.indentation.afterComma, ' ')
              }
              // remove the final comma and apply proper indentation
              if (fields.nonEmpty) {
                buffer.setLength(buffer.length() - (1 + (if (format.indentation.afterComma) 1 else 0)))
                newlineAndIndent(depth)
                appendIf(!format.indentation.newlines && format.indentation.padObject, ' ')
              }
              buffer.append('}')
          }
        }
    }
    // only return if the max length is not exceeded
    breakIfMaxLengthExceeded()
  }

  /**
   * Prints the bson in a human-readable optimized format.
   */
  def pretty(bson: BsonValue): String = stringify(bson)(StringifyFormat.pretty)

  /**
   * Prints the bson in the least amount of characters on a single line.
   */
  def inline(bson: BsonValue): String = stringify(bson)(StringifyFormat.inline)

  /**
   * Prints the bson using the custom style provided implicitly.
   */
  def stringify(bson: BsonValue)(implicit format: StringifyFormat): String = stringifyInternal(bson, 0)

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
