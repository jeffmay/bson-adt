package adt.bson.mongo.client.model

import adt.bson.{Bson, BsonObject, BsonObjectWrites}
import com.mongodb.client.model._

import scala.language.implicitConversions

/**
 * A base class for models that can be used in a bulk write operations.
 */
sealed abstract class WriteModel {
  def asJava: JavaWriteModel[BsonObject]
}
object WriteModel {
  implicit def toWriteModel(model: WriteModel): JavaWriteModel[BsonObject] = model.asJava
}

/**
 * A model describing the removal of at most one document matching the query filter.
 */
case class DeleteOneModel(filter: BsonObject) extends WriteModel {
  override def asJava: JavaDeleteOneModel[BsonObject] = new JavaDeleteOneModel(filter)
}

/**
 * A model describing the removal of all documents matching the query filter.
 */
case class DeleteManyModel(filter: BsonObject) extends WriteModel {
  override def asJava: JavaDeleteManyModel[BsonObject] = new JavaDeleteManyModel(filter)
}

/**
 * A model describing an insert of a single document.
 */
case class InsertOneModel(document: BsonObject) extends WriteModel {
  override def asJava: JavaInsertOneModel[BsonObject] = new JavaInsertOneModel(document)
}
object InsertOneModel {
  def apply[T: BsonObjectWrites](document: T): InsertOneModel = new InsertOneModel(Bson.toBsonObject(document))
}

/**
 * A model describing the replacement of at most one document that matches the query filter.
 *
 * @param filter a query to filter the documents
 * @param replacement the document to replace the matched documents with
 * @param options additional [[UpdateOptions]]
 */
case class ReplaceOneModel(
  filter: BsonObject,
  replacement: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def asJava: JavaReplaceOneModel[BsonObject] = new JavaReplaceOneModel(filter, replacement, options)
}
object ReplaceOneModel {
  def apply[T: BsonObjectWrites](filter: BsonObject, document: T, options: UpdateOptions): ReplaceOneModel =
    new ReplaceOneModel(filter, Bson.toBsonObject(document), options)
}

/**
 * A model describing an update to at most one document that matches the query filter.
 * The update to apply must include only update operators.
 *
 * @param filter a query to locate a single document
 * @param update an update command to apply to that document
 * @param options additional [[UpdateOptions]]
 */
case class UpdateOneModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def asJava: JavaUpdateOneModel[BsonObject] = new JavaUpdateOneModel(filter, update, options)
}

/**
 * A model describing an update to all documents that matches the query filter.
 * The update to apply must include only update operators.
 *
 * @param filter a query to locate a matching set of documents
 * @param update an update command to apply to that document
 * @param options additional [[UpdateOptions]]
 */
case class UpdateManyModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends WriteModel {
  override def asJava: JavaUpdateManyModel[BsonObject] = new JavaUpdateManyModel(filter, update, options)
}
