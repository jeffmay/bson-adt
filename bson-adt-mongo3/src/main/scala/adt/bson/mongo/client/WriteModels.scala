package adt.bson.mongo.client

import adt.bson.{Bson, BsonObject, BsonObjectWrites}
import com.mongodb.client.model.UpdateOptions

import scala.language.implicitConversions

/**
 * A base class for models that can be used in a bulk write operations.
 *
 * @note substitutes [[com.mongodb.client.model.WriteModel]]
 */
sealed abstract class WriteModel {
  def toJavaWriteModel: JavaWriteModel[BsonObject]
}
object WriteModel {
  implicit def toJavaWriteModel(model: WriteModel): JavaWriteModel[BsonObject] = model.toJavaWriteModel
}

/**
 * A model describing the removal of at most one document matching the query filter.
 *
 * @note substitutes [[com.mongodb.client.model.DeleteOneModel]]
 *
 * @param filter an object describing the query filter.
 */
case class DeleteOneModel(filter: BsonObject) extends WriteModel {
  override def toJavaWriteModel: JavaDeleteOneModel[BsonObject] = new JavaDeleteOneModel(filter)
}

/**
 * A model describing the removal of all documents matching the query filter.
 *
 * @note substitutes [[com.mongodb.client.model.DeleteManyModel]]
 *
 * @param filter an object describing the query filter.
 */
case class DeleteManyModel(filter: BsonObject) extends WriteModel {
  override def toJavaWriteModel: JavaDeleteManyModel[BsonObject] = new JavaDeleteManyModel(filter)
}

/**
 * A model describing an insert of a single document.
 *
 * @note substitutes [[com.mongodb.client.model.InsertOneModel]]
 *
 * @param document the document to insert
 */
case class InsertOneModel(document: BsonObject) extends WriteModel {
  override def toJavaWriteModel: JavaInsertOneModel[BsonObject] = new JavaInsertOneModel(document)
}
object InsertOneModel {
  def apply[T: BsonObjectWrites](document: T): InsertOneModel = new InsertOneModel(Bson.toBsonObject(document))
}

/**
 * A model describing the replacement of at most one document that matches the query filter.
 *
 * @note substitutes [[com.mongodb.client.model.ReplaceOneModel]]
 *
 * @param filter an object describing the query filter
 * @param replacement the replacement document
 * @param options the options to apply
 */
case class ReplaceOneModel(
  filter: BsonObject,
  replacement: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def toJavaWriteModel: JavaReplaceOneModel[BsonObject] = new JavaReplaceOneModel(filter, replacement, options)
}
object ReplaceOneModel {
  def apply[T: BsonObjectWrites](filter: BsonObject, document: T, options: UpdateOptions): ReplaceOneModel =
    new ReplaceOneModel(filter, Bson.toBsonObject(document), options)
}

/**
 * A model describing an update to at most one document that matches the query filter.
 * The update to apply must include only update operators.
 *
 * @note substitutes [[com.mongodb.client.model.UpdateOneModel]]
 *
 * @param filter an object describing the query filter
 * @param update a document describing the update. The update to apply must include only update operators.
 * @param options the options to apply
 */
case class UpdateOneModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def toJavaWriteModel: JavaUpdateOneModel[BsonObject] = new JavaUpdateOneModel(filter, update, options)
}

/**
 * A model describing an update to all documents that matches the query filter.
 * The update to apply must include only update operators.
 *
 * @note substitutes [[com.mongodb.client.model.UpdateManyModel]]
 *
 * @param filter an object describing the query filter
 * @param update a document describing the update. The update to apply must include only update operators.
 * @param options the options to apply
 */
case class UpdateManyModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def toJavaWriteModel: JavaUpdateManyModel[BsonObject] = new JavaUpdateManyModel(filter, update, options)
}
