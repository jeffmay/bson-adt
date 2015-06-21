package adt.bson.mongo.bulk

import adt.bson.BsonObject

import scala.language.implicitConversions

/**
 * An abstract base class for a write request (substitutes [[com.mongodb.bulk.WriteRequest]]).
 */
sealed trait WriteRequest {
  def toJavaWriteRequest: JavaWriteRequest
}
object WriteRequest {
  implicit def from(request: JavaWriteRequest): WriteRequest = request match {
    case delete: JavaDeleteRequest => DeleteRequest(delete.getFilter.toBsonObject, delete.isMulti)
    case insert: JavaInsertRequest => InsertRequest(insert.getDocument.toBsonObject)
    case update: JavaUpdateRequest if update.getType == JavaWriteRequest.Type.UPDATE =>
      UpdateRequest(update.getFilter.toBsonObject, update.getUpdate.toBsonObject, update.isMulti, update.isUpsert)
    case replace: JavaUpdateRequest if replace.getType == JavaWriteRequest.Type.REPLACE =>
      ReplaceRequest(replace.getFilter.toBsonObject, replace.getUpdate.toBsonObject, replace.isUpsert)
  }
}

/**
 * A representation of a delete (substitutes [[com.mongodb.bulk.WriteRequest]]).
 *
 * @param filter the query filter
 * @param multi whether to delete multiple documents matching this filter
 */
case class DeleteRequest(filter: BsonObject, multi: Boolean = true) extends WriteRequest {
  final override def toJavaWriteRequest: JavaDeleteRequest =
    new JavaDeleteRequest(filter.toJavaBsonDocument).multi(multi)
}

/**
 * A representation of a document to insert (substitutes [[com.mongodb.bulk.InsertRequest]]).
 *
 * @param document the document to insert
 */
case class InsertRequest(document: BsonObject) extends WriteRequest {
  final override def toJavaWriteRequest: JavaInsertRequest = new JavaInsertRequest(document.toJavaBsonDocument)
}

/**
 * An update to one or more documents (substitutes [[com.mongodb.bulk.UpdateRequest]] with an update command).
 *
 * @param filter the query filter
 * @param update the update operations
 * @param multi whether this update will update all documents matching the filter
 * @param upsert whether this update will insert a new document if no documents match the filter
 */
case class UpdateRequest(
  filter: BsonObject,
  update: BsonObject,
  multi: Boolean = true,
  upsert: Boolean = false) extends WriteRequest {

  final override def toJavaWriteRequest: JavaUpdateRequest =
    new JavaUpdateRequest(
      filter.toJavaBsonDocument,
      update.toJavaBsonDocument,
      JavaWriteRequest.Type.UPDATE)
      .multi(multi)
      .upsert(upsert)

  final def toReplaceRequest: ReplaceRequest = ReplaceRequest(filter, update, upsert)
}

/**
 * An update to one or more documents (substitutes [[com.mongodb.bulk.UpdateRequest]] with a replace command).
 *
 * @param filter the query filter
 * @param update the update operations
 * @param upsert whether this update will insert a new document if no documents match the filter
 */
case class ReplaceRequest(
  filter: BsonObject,
  update: BsonObject,
  upsert: Boolean = false) extends WriteRequest {

  final override def toJavaWriteRequest: JavaUpdateRequest =
    new JavaUpdateRequest(
      filter.toJavaBsonDocument,
      update.toJavaBsonDocument,
      JavaWriteRequest.Type.REPLACE)
      .upsert(upsert)

  def toUpdateRequest: UpdateRequest = UpdateRequest(filter, update, upsert = upsert)
}
