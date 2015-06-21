package adt.bson.mongo.codecs

import adt.bson.mongo._
import adt.bson.{BsonFormat, BsonValue, _}
import org.bson._
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * Provides implicit conversions between [[Codec]] and [[BsonFormat]], as well as some default implementations.
 *
 * Enables [[Codec]]'s be used implicitly, in case you need to support cross-compatibility
 * and want to utilize implicits.
 */
object BsonCodecImplicits extends BsonCodecImplicits

trait BsonCodecImplicits extends BsonCodecLowPriorityImplicits {

  implicit def bsonValueCodec: Codec[BsonValue] = BsonAdtCodec

  implicit def codecRegistry: CodecRegistry = BsonAdtCodecRegistry

  /**
   * Uses the [[BsonAdtImplicits]] to read and write [[JavaBsonValue]]s.
   */
  implicit object JavaBsonValueFormat extends BsonFormat[JavaBsonValue] {
    override def writes(javaBson: JavaBsonValue): BsonValue = javaBson.toBson
    override def reads(adtBson: BsonValue): JavaBsonValue = adtBson.toJavaBson
  }
}
trait BsonCodecLowPriorityImplicits {

  /**
   * Convert a [[Codec]] into a [[BsonFormat]] implicitly.
   *
   * If you would like to make a Mongo value convertible into a [[BsonValue]], just assign it
   * to an implicit [[BsonFormat]] of the right type and it will convert appropriately.
   */
  implicit def format[T](codec: Codec[T]): BsonFormat[T] = new BsonFormat[T] {
    override def writes(value: T): BsonValue = {
      val writer = new BsonDocumentWriter(new JavaBsonDocument())
      writer.writeName("obj")
      codec.encode(writer, value, EncoderContext.builder().build())
      val reader = new BsonDocumentReader(writer.getDocument)
      reader.readName("obj")
      val bson = BsonAdtCodec.decode(reader, DecoderContext.builder().build())
      bson
    }
    override def reads(bson: BsonValue): T = {
      val writer = new BsonDocumentWriter(new JavaBsonDocument())
      writer.writeName("obj")
      BsonAdtCodec.encode(writer, bson, EncoderContext.builder().build())
      val reader = new BsonDocumentReader(writer.getDocument)
      reader.readName("obj")
      val value = codec.decode(reader, DecoderContext.builder().build())
      value
    }
  }

  /**
   * Convert a [[BsonFormat]] into a [[Codec]] implicitly.
   *
   * This allows you to pass a [[BsonFormat]] as a [[Codec]]. This is equivalent to passing [[BsonAdtCodec]],
   * unless you are building a more granular [[org.bson.codecs.configuration.CodecRegistry]].
   */
  implicit def codec[T](format: BsonFormat[T])(implicit tag: ClassTag[T]): Codec[T] = new Codec[T] {
    override def decode(reader: BsonReader, decoderContext: DecoderContext): T = {
      val value = BsonAdtCodec.decode(reader, decoderContext)
      format reads value
    }
    override def encode(writer: BsonWriter, value: T, encoderContext: EncoderContext): Unit = {
      val bson = format writes value
      BsonAdtCodec.encode(writer, bson, encoderContext)
    }
    override def getEncoderClass: Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
  }
}
