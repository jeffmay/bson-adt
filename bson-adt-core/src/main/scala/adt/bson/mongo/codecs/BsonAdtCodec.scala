package adt.bson.mongo.codecs

import adt.bson._
import org.bson.codecs._
import org.bson.{BsonReader, BsonRegularExpression, BsonType, BsonWriter}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

/**
 * A single [[Codec]] for the entire [[BsonValue]] algebraic data type.
 *
 * While it would be possible to use a [[org.bson.codecs.configuration.CodecProvider]] that maps all the
 * classes over to separate [[Codec]]s and then reading from that, using pattern matching is much faster
 * and simpler mechanism.
 *
 * If you want to provide your own [[Codec]], you have to create the [[org.bson.codecs.configuration.CodecRegistry]]
 * with your own [[org.bson.codecs.configuration.CodecProvider]]s, and place them before the [[BsonAdtCodecProvider]]
 *
 * However, writing your own [[Codec]]s is usually unnecessary, as you can convert a [[BsonValue]] to your
 * desired result type using an [[adt.bson.BsonReads]] for that type.
 */
object BsonAdtCodec extends BsonAdtCodec
class BsonAdtCodec extends Codec[BsonValue] {

  override def encode(writer: BsonWriter, bson: BsonValue, encoderContext: EncoderContext): Unit = bson match {
    case BsonString(value)   => writer.writeString(value)
    case BsonBoolean(value)  => writer.writeBoolean(value)
    case BsonInt(value)      => writer.writeInt32(value)
    case BsonLong(value)     => writer.writeInt64(value)
    case BsonDouble(value)   => writer.writeDouble(value)
    case BsonNull            => writer.writeNull()
    case BsonObjectId(value) => writer.writeObjectId(value)
    case BsonDate(value)     => writer.writeDateTime(value.getMillis)
    case BsonArray(values)   =>
      writer.writeStartArray()
      for (value <- values) {
        encoderContext.encodeWithChildContext(this, writer, value)
      }
      writer.writeEndArray()
    case BsonObject(fields)  =>
      writer.writeStartDocument()
      for ((k, v) <- fields) {
        writer.writeName(k)
        encoderContext.encodeWithChildContext(this, writer, v)
      }
      writer.writeEndDocument()
    case BsonBinary(value)   => writer.writeBinaryData(JavaBsonBinary(value))
    case BsonRegex(value)    => writer.writeRegularExpression(new BsonRegularExpression(value.pattern.pattern))
    case BsonUndefined()     => writer.writeUndefined()
  }

  override def getEncoderClass: Class[BsonValue] = classOf[BsonValue]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): BsonValue = {
    val bsonType = Option(reader.getCurrentBsonType)
    readValue(bsonType getOrElse BsonType.DOCUMENT, reader, decoderContext)
  }

  def readValue(bsonType: BsonType, reader: BsonReader, decoderContext: DecoderContext): BsonValue = {
    bsonType match {
      case BsonType.STRING             => BsonString(reader.readString())
      case BsonType.BOOLEAN            => BsonBoolean(reader.readBoolean())
      case BsonType.DOUBLE             => BsonDouble(reader.readDouble())
      case BsonType.INT32              => BsonInt(reader.readInt32())
      case BsonType.INT64              => BsonLong(reader.readInt64())
      case BsonType.OBJECT_ID          => BsonObjectId(reader.readObjectId())
      case BsonType.DATE_TIME          => BsonDate(new DateTime(reader.readDateTime(), DateTimeZone.UTC))
      case BsonType.NULL               =>
        reader.readNull()
        BsonNull
      case BsonType.ARRAY              =>
        reader.readStartArray()
        val elements = ListBuffer[BsonValue]()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          elements.append(decode(reader, decoderContext))
        }
        reader.readEndArray()
        BsonArray(elements.toIndexedSeq)
      case BsonType.DOCUMENT           =>
        reader.readStartDocument()
        val fields = ListBuffer[(String, BsonValue)]()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
          val key = reader.readName()
          val value = decode(reader, decoderContext)
          fields.append(key -> value)
        }
        reader.readEndDocument()
        BsonObject(fields.toMap)
      case BsonType.BINARY             => BsonBinary(reader.readBinaryData().getData)
      case BsonType.REGULAR_EXPRESSION =>
        val pattern = reader.readRegularExpression().getPattern
        BsonRegex(pattern.r)
      case BsonType.UNDEFINED          =>
        reader.readUndefined()
        BsonUndefined("undefined in document")
      case tpe => throw new UnsupportedOperationException(s"Unsupported BsonValue type: '${tpe.name}'")
    }
  }
}
