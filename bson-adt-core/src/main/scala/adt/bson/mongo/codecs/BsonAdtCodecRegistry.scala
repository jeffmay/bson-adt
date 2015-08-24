package adt.bson.mongo.codecs

import adt.bson.{BsonValue, BsonObject}
import org.bson.codecs.{ValueCodecProvider, DocumentCodecProvider, Codec}
import org.bson.codecs.configuration.{CodecProvider, CodecConfigurationException, CodecRegistry}

/**
 * A [[CodecRegistry]] that is optimized to provide the [[BsonAdtCodec]] for all subclasses of [[BsonValue]].
 *
 * This is used for helper code that requires a [[CodecRegistry]]. You should only use this
 * object if you know that you are only dealing with [[BsonValue]] subclasses.
 *
 * If you want the ability to add more [[Codec]]s to a registry, then you should consider creating
 * a [[CodecRegistry]] using [[org.bson.codecs.configuration.CodecRegistries.fromProviders]] and
 * passing the [[BsonAdtCodecProvider]].
 *
 * However, writing your own [[Codec]]s is usually unnecessary, as you can convert a [[BsonValue]] to your
 * desired result type using an [[adt.bson.BsonReads]] for that type.
 */
object BsonAdtCodecRegistry extends BsonAdtCodecRegistry(Seq(new DocumentCodecProvider, new ValueCodecProvider))
class BsonAdtCodecRegistry(providers: Seq[CodecProvider]) extends CodecRegistry {

  private val BsonObjectClass = classOf[BsonObject]
  private val BsonValueClass = classOf[BsonValue]

  override def get[T](clazz: Class[T]): Codec[T] = {
    if (clazz == BsonObjectClass || clazz == BsonValueClass || clazz.isAssignableFrom(BsonValueClass)) {
      BsonAdtCodec.asInstanceOf[Codec[T]]
    }
    else providers.view.map(_.get(clazz, this)).filterNot(_ eq null).headOption.getOrElse {
      throw new CodecConfigurationException(s"${clazz.getName} is not registered in the BsonAdtCodecRegistry. " +
        s"Only subclasses of ${BsonValueClass.getName} have codecs in this registry")
    }
  }
}
