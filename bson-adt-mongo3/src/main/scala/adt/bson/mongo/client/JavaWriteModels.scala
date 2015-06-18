package adt.bson.mongo.client

object JavaWriteModels extends JavaWriteModels
trait JavaWriteModels {
  type JavaWriteModel[T]      = com.mongodb.client.model.WriteModel[T]
  type JavaDeleteManyModel[T] = com.mongodb.client.model.DeleteManyModel[T]
  type JavaDeleteOneModel[T]  = com.mongodb.client.model.DeleteOneModel[T]
  type JavaInsertOneModel[T]  = com.mongodb.client.model.InsertOneModel[T]
  type JavaReplaceOneModel[T] = com.mongodb.client.model.ReplaceOneModel[T]
  type JavaUpdateManyModel[T] = com.mongodb.client.model.UpdateManyModel[T]
  type JavaUpdateOneModel[T]  = com.mongodb.client.model.UpdateOneModel[T]
}
