package adt.bson.mongo.codecs

import adt.bson.{BsonObject, BsonValue}
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}

/**
 * Provides the [[BsonAdtCodec]] for [[BsonValue]].
 *
 * Be sure to `import adt.bson.mongo._` to be able to convert to and from
 *
 * @note some operations on clients return [[org.bson.BsonDocument]] instead of [[BsonObject]],
 *       to address this you can include [[org.bson.codecs.DocumentCodecProvider]]
 *       and [[org.bson.codecs.ValueCodecProvider]] to safely call these methods.
 *
 * To create a [[CodecRegistry]] for this, you can use:
 * {{{
 *   CodecRegistries.fromProviders(BsonAdtCodecProvider, new DocumentCodecProvider(), new ValueCodecProvider())
 * }}}
 */
object BsonAdtCodecProvider extends CodecProvider {

  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
    if (clazz == classOf[BsonObject] || clazz == classOf[BsonValue]) BsonAdtCodec.asInstanceOf[Codec[T]]
    else null  // as the documentation states, a null value means to skip this provider
  }
}
