package adt.bson.mongo.client.model

import adt.bson.BsonObject
import com.mongodb.client.model.IndexOptions
import org.bson.codecs.configuration.CodecRegistry

import scala.language.implicitConversions

/**
 * A model describing the creation of a single index (substitutes [[com.mongodb.client.model.IndexModel]]).
 */
case class IndexModel(keys: BsonObject, options: IndexOptions = new IndexOptions) {
  def toJavaModel: JavaIndexModel = new JavaIndexModel(keys, options)
}

object IndexModel {
  implicit def from(model: JavaIndexModel)(implicit registry: CodecRegistry): IndexModel =
    IndexModel(model.getKeys.toBsonObject)
}
