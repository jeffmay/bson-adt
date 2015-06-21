package adt.bson.mongo.codecs

import adt.bson.{BsonValue, BsonObject}
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecConfigurationException, CodecRegistry}

object BsonAdtCodecRegistry extends CodecRegistry {

  private val BsonObjectClass = classOf[BsonObject]
  private val BsonValueClass = classOf[BsonValue]

  override def get[T](clazz: Class[T]): Codec[T] = {
    if (clazz == BsonObjectClass || clazz == BsonValueClass || clazz.isAssignableFrom(BsonValueClass)) {
      BsonAdtCodec.asInstanceOf[Codec[T]]
    }
    else throw new CodecConfigurationException(s"${clazz.getName} is not registered in the BsonAdtCodecRegistry. " +
      s"Only subclasses of adt.bson.BsonObject have codecs in this registry")
  }
}
