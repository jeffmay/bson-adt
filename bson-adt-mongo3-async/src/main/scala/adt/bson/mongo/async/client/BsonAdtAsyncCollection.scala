package adt.bson.mongo.async.client

import java.lang.Long

import adt.bson.mongo.MongoSingleCallback
import adt.bson.mongo.client.model.{WriteModel, IndexModel, _}
import adt.bson.mongo.client.result._
import adt.bson.mongo.bulk._
import adt.bson.{BsonAdtImplicits, BsonJavaScript, BsonObject, BsonValue}
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoCollection
import com.mongodb.client.model._
import com.mongodb.{MongoNamespace, ReadPreference, WriteConcern}
import org.bson.codecs.configuration.CodecRegistry
import play.api.libs.functional.syntax._

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList}
import scala.concurrent.{Future, Promise}
import scala.util.Try

object BsonAdtAsyncCollection {

  type Callback[P] = Try[P] => Unit

  def apply(collection: MongoCollection[BsonObject]): BsonAdtAsyncCollection = new BsonAdtAsyncCollection(collection)
}

class BsonAdtAsyncCollection(underlying: MongoCollection[BsonObject]) extends BsonAdtImplicits {

  private def promise[P](action: SingleResultCallback[P] => Any): Future[P] = {
    val p = Promise[P]()
    action(MongoSingleCallback.complete(p))
    p.future
  }

  private def promiseUnit(action: SingleResultCallback[Void] => Any): Future[Unit] = {
    val p = Promise[Unit]()
    action(MongoSingleCallback.complete(p).contramap(_ => ()))
    p.future
  }

  def count(): Future[Long] = promise(underlying.count)

  def count(filter: BsonObject): Future[Long] = promise[Long](underlying.count(filter, _))

  def count(filter: BsonObject, options: CountOptions): Future[Long] = promise[Long](underlying.count(filter, options, _))

  def aggregate(pipeline: Seq[BsonObject]): ScalaAggregateIterable[BsonObject] =
    new ScalaAggregateIterable(underlying.aggregate(pipeline))

  def distinct(fieldName: String): ScalaDistinctIterable[BsonValue] =
    new ScalaDistinctIterable(underlying.distinct(fieldName, classOf[BsonValue]))

  def updateMany(filter: BsonObject, update: BsonObject): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.updateMany(filter, update, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def updateMany(filter: BsonObject, update: BsonObject, options: UpdateOptions): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.updateMany(filter, update, options, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def createIndex(key: BsonObject): Future[String] = promise[String](underlying.createIndex(key, _))

  def createIndex(key: BsonObject, options: IndexOptions): Future[String] =
    promise[String](underlying.createIndex(key, options, _))

  def findOneAndDelete(filter: BsonObject): Future[BsonObject] =
    promise[BsonObject](underlying.findOneAndDelete(filter, _))

  def findOneAndDelete(filter: BsonObject, options: FindOneAndDeleteOptions): Future[BsonObject] =
    promise[BsonObject](underlying.findOneAndDelete(filter, options, _))

  def dropIndexes(): Future[Unit] = promiseUnit(underlying.dropIndexes)

  def findOneAndReplace(filter: BsonObject, replacement: BsonObject): Future[BsonObject] = {
    val promise = Promise[BsonObject]()
    underlying.findOneAndReplace(filter, replacement, MongoSingleCallback.complete(promise))
    promise.future
  }

  def findOneAndReplace(filter: BsonObject, replacement: BsonObject, options: FindOneAndReplaceOptions): Future[BsonObject] = {
    val promise = Promise[BsonObject]()
    underlying.findOneAndReplace(filter, replacement, options, MongoSingleCallback.complete(promise))
    promise.future
  }

  def codecRegistry: CodecRegistry = underlying.getCodecRegistry

  def withReadPreference(readPreference: ReadPreference): BsonAdtAsyncCollection =
    new BsonAdtAsyncCollection(underlying.withReadPreference(readPreference))

  def drop(): Future[Unit] = promiseUnit(underlying.drop)

  def writeConcern: WriteConcern = underlying.getWriteConcern

  def namespace: MongoNamespace = underlying.getNamespace

  def insertMany(documents: Seq[BsonObject]): Future[Unit] = promiseUnit(underlying.insertMany(documents, _))

  def insertMany(documents: Seq[BsonObject], options: InsertManyOptions): Future[Unit] =
    promiseUnit(underlying.insertMany(documents, options, _))

  def deleteMany(filter: BsonObject): Future[DeleteResult] = {
    val promise = Promise[DeleteResult]()
    underlying.deleteMany(filter, MongoSingleCallback.complete(promise).contramap(new DeleteResult(_)))
    promise.future
  }

  def mapReduce(mapFunction: BsonJavaScript, reduceFunction: BsonJavaScript): ScalaMapReduceIterable[BsonObject] =
    new ScalaMapReduceIterable(underlying.mapReduce(mapFunction.value, reduceFunction.value))

  def bulkWrite(requests: Seq[WriteModel]): Future[BulkWriteResult] = {
    val promise = Promise[BulkWriteResult]()
    underlying.bulkWrite(requests.map(_.asJava), MongoSingleCallback.complete(promise).contramap(BulkWriteResult.from))
    promise.future
  }

  def bulkWrite(requests: Seq[WriteModel], options: BulkWriteOptions): Future[BulkWriteResult] = {
    val promise = Promise[BulkWriteResult]()
    underlying.bulkWrite(
      requests.map(_.asJava),
      options,
      MongoSingleCallback.complete(promise).contramap(BulkWriteResult.from)
    )
    promise.future
  }

  def listIndexes(): ScalaListIndexesIterable[BsonValue] =
    new ScalaListIndexesIterable(underlying.listIndexes(classOf[BsonValue]))

  def readPreference: ReadPreference = underlying.getReadPreference

  def insertOne(document: BsonObject): Future[Unit] = promiseUnit(underlying.insertOne(document, _))

  def updateOne(filter: BsonObject, update: BsonObject): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.updateOne(filter, update, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def updateOne(filter: BsonObject, update: BsonObject, options: UpdateOptions): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.updateOne(filter, update, options, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def findOneAndUpdate(filter: BsonObject, update: BsonObject): Future[BsonObject] =
    promise[BsonObject](underlying.findOneAndUpdate(filter, update, _))

  def findOneAndUpdate(filter: BsonObject, update: BsonObject, options: FindOneAndUpdateOptions): Future[BsonObject] =
    promise[BsonObject](underlying.findOneAndUpdate(filter, update, options, _))

  def withWriteConcern(writeConcern: WriteConcern): BsonAdtAsyncCollection =
    new BsonAdtAsyncCollection(underlying.withWriteConcern(writeConcern))

  def find(): ScalaFindIterable[BsonObject] = new ScalaFindIterable(underlying.find())

  def find(filter: BsonObject): ScalaFindIterable[BsonObject] = new ScalaFindIterable(underlying.find(filter))

  def withCodecRegistry(codecRegistry: CodecRegistry): BsonAdtAsyncCollection =
    new BsonAdtAsyncCollection(underlying.withCodecRegistry(codecRegistry))

  def createIndexes(indexes: Seq[IndexModel]): Future[Seq[String]] = {
    val promise = Promise[Seq[String]]()
    underlying.createIndexes(indexes.map(_.toJavaModel), MongoSingleCallback.complete(promise).contramap(_.toSeq))
    promise.future
  }

  def replaceOne(filter: BsonObject, replacement: BsonObject): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.replaceOne(filter, replacement, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def replaceOne(filter: BsonObject, replacement: BsonObject, options: UpdateOptions): Future[UpdateResult] = {
    val promise = Promise[UpdateResult]()
    underlying.replaceOne(filter, replacement, options, MongoSingleCallback.complete(promise).contramap(UpdateResult.from))
    promise.future
  }

  def dropIndex(indexName: String): Future[Unit] = promiseUnit(underlying.dropIndex(indexName, _))

  def dropIndex(keys: BsonObject): Future[Unit] = promiseUnit(underlying.dropIndex(keys, _))

  def deleteOne(filter: BsonObject): Future[DeleteResult] = {
    val promise = Promise[DeleteResult]()
    underlying.deleteOne(filter, MongoSingleCallback.complete(promise).contramap(DeleteResult.from))
    promise.future
  }

  def renameCollection(newCollectionNamespace: MongoNamespace): Future[Unit] =
    promiseUnit(underlying.renameCollection(newCollectionNamespace, _))

  def renameCollection(newCollectionNamespace: MongoNamespace, options: RenameCollectionOptions): Future[Unit] = {
    promiseUnit(underlying.renameCollection(newCollectionNamespace, _))
  }

  def toJavaCollection: MongoCollection[BsonObject] = underlying
}
