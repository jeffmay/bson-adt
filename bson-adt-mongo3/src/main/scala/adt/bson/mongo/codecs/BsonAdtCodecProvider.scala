package adt.bson.mongo.codecs

import adt.bson.BsonObject
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}

/**
 * Provides the [[BsonAdtCodec]] for [[com.mongodb.client.MongoCollection]] of type [[BsonObject]].
 *
 * @note some operations in [[com.mongodb.MongoClient]] are not parameterized to return [[BsonObject]]
 *       once this is configured. You will still need to include [[org.bson.codecs.DocumentCodecProvider]]
 *       and [[org.bson.codecs.ValueCodecProvider]] to call these methods.
 *
 * To create a [[CodecRegistry]] for this, you can use:
 * {{{
 *   CodecRegistries.fromProviders(BsonAdtCodecProvider, new DocumentCodecProvider(), new ValueCodecProvider())
 * }}}
 */
object BsonAdtCodecProvider extends CodecProvider {

  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
    if (clazz == classOf[BsonObject]) BsonAdtCodec.asInstanceOf[Codec[T]]
    else null  // as the documentation states, a null value means to skip this provider
  }
}
