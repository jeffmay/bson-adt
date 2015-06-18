package adt.bson.mongo.client

import adt.bson.{Bson, BsonObject, BsonObjectWrites}
import com.mongodb.client.model.{UpdateOptions, WriteModel}

import scala.language.implicitConversions

sealed abstract class ScalaWriteModel {
  def toJavaWriteModel: WriteModel[BsonObject]
}
object ScalaWriteModel {

  implicit def toWriteModel(model: ScalaWriteModel): WriteModel[BsonObject] = model.toJavaWriteModel
}

case class DeleteOneModel(filter: BsonObject) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaDeleteOneModel[BsonObject] = new JavaDeleteOneModel(filter)
}

case class DeleteManyModel(filter: BsonObject) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaDeleteManyModel[BsonObject] = new JavaDeleteManyModel(filter)
}

case class InsertOneModel(document: BsonObject) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaInsertOneModel[BsonObject] = new JavaInsertOneModel(document)
}
object InsertOneModel {
  def apply[T: BsonObjectWrites](document: T): InsertOneModel = new InsertOneModel(Bson.toBsonObject(document))
}

case class ReplaceOneModel(
  filter: BsonObject,
  replacement: BsonObject,
  options: UpdateOptions
  ) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaReplaceOneModel[BsonObject] = new JavaReplaceOneModel(filter, replacement, options)
}
object ReplaceOneModel {
  def apply[T: BsonObjectWrites](filter: BsonObject, document: T, options: UpdateOptions): ReplaceOneModel =
    new ReplaceOneModel(filter, Bson.toBsonObject(document), options)
}

case class UpdateOneModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaUpdateOneModel[BsonObject] = new JavaUpdateOneModel(filter, update, options)
}

case class UpdateManyModel(
  filter: BsonObject,
  update: BsonObject,
  options: UpdateOptions
  ) extends ScalaWriteModel {
  override def toJavaWriteModel: JavaUpdateManyModel[BsonObject] = new JavaUpdateManyModel(filter, update, options)
}
