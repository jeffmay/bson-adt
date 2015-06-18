package adt.bson.mongo.client

import adt.bson._
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client._
import com.mongodb.client.model._
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import com.mongodb.{MongoNamespace, ReadPreference, WriteConcern}
import org.bson.codecs.configuration.CodecRegistry

import scala.collection.JavaConversions.{iterableAsScalaIterable, seqAsJavaList}
import scala.language.implicitConversions

object BsonAdtCollection {

  def apply(collection: MongoCollection[BsonObject]): BsonAdtCollection = new BsonAdtCollection(collection)
}

class BsonAdtCollection(underlying: MongoCollection[BsonObject]) extends BsonAdtImplicits {

  def count(): Long = underlying.count()

  def count(filter: BsonObject): Long = underlying.count(filter)

  def count(filter: BsonObject, options: CountOptions): Long = underlying.count(filter, options)

  def aggregate(pipeline: Seq[BsonObject]): AggregateIterable[BsonObject] =
    new AggregateIterable(underlying.aggregate(pipeline))

  def distinct(fieldName: String): DistinctIterable[BsonValue] =
    new DistinctIterable(underlying.distinct(fieldName, classOf[BsonValue]))

  def updateMany(filter: BsonObject, update: BsonObject): UpdateResult =
    underlying.updateMany(filter, update)

  def updateMany(filter: BsonObject, update: BsonObject, updateOptions: UpdateOptions): UpdateResult =
    underlying.updateMany(filter, update, updateOptions)

  def createIndex(keys: BsonObject): String = underlying.createIndex(keys)

  def createIndex(keys: BsonObject, indexOptions: IndexOptions): String = underlying.createIndex(keys, indexOptions)

  def findOneAndDelete(filter: BsonObject): BsonObject = underlying.findOneAndDelete(filter)

  def findOneAndDelete(filter: BsonObject, options: FindOneAndDeleteOptions): BsonObject =
    underlying.findOneAndDelete(filter, options)

  def dropIndexes(): Unit = underlying.dropIndexes()

  def findOneAndReplace(filter: BsonObject, replacement: BsonObject): BsonObject =
    underlying.findOneAndReplace(filter, replacement)

  def findOneAndReplace(filter: BsonObject, replacement: BsonObject, options: FindOneAndReplaceOptions): BsonObject =
    underlying.findOneAndReplace(filter, replacement, options)

  def codecs: CodecRegistry = underlying.getCodecRegistry

  def withReadPreference(readPreference: ReadPreference): BsonAdtCollection =
    new BsonAdtCollection(underlying.withReadPreference(readPreference))

  def writeConcern: WriteConcern = underlying.getWriteConcern

  def drop(): Unit = underlying.drop()

  def namespace: MongoNamespace = underlying.getNamespace

  def insertMany(documents: Seq[BsonObject]): Unit = underlying.insertMany(documents)

  def insertMany(documents: Seq[BsonObject], options: InsertManyOptions): Unit =
    underlying.insertMany(documents, options)

  def deleteMany(filter: BsonObject): DeleteResult = underlying.deleteMany(filter)

  def mapReduce(mapFunction: BsonJavaScript, reduceFunction: BsonJavaScript): MapReduceIterable[BsonObject] =
    new MapReduceIterable(underlying.mapReduce(mapFunction.value, reduceFunction.value))

  def bulkWrite(requests: Seq[WriteModel]): BulkWriteResult =
    underlying.bulkWrite(requests.map(_.toJavaWriteModel))

  def bulkWrite(requests: Seq[WriteModel], options: BulkWriteOptions): BulkWriteResult =
    underlying.bulkWrite(requests.map(_.toJavaWriteModel))

  def documentClass: Class[BsonObject] = classOf[BsonObject]

  def listIndexes(): ListIndexesIterable[BsonObject] =
    new ListIndexesIterable(underlying.listIndexes(classOf[BsonObject]))

  def readPreference: ReadPreference = underlying.getReadPreference

  def insertOne(document: BsonObject): Unit = underlying.insertOne(document)

  def updateOne(filter: BsonObject, update: BsonObject): UpdateResult = underlying.updateOne(filter, update)

  def updateOne(filter: BsonObject, update: BsonObject, updateOptions: UpdateOptions): UpdateResult =
    underlying.updateOne(filter, update, updateOptions)

  def withWriteConcern(writeConcern: WriteConcern): BsonAdtCollection =
    new BsonAdtCollection(underlying.withWriteConcern(writeConcern))

  def findOneAndUpdate(filter: BsonObject, update: BsonObject): BsonObject = underlying.findOneAndUpdate(filter, update)

  def findOneAndUpdate(filter: BsonObject, update: BsonObject, options: FindOneAndUpdateOptions): BsonObject =
    underlying.findOneAndUpdate(filter, update, options)

  def find(): FindIterable[BsonObject] = new FindIterable(underlying.find())

  def find(filter: BsonObject): FindIterable[BsonObject] = new FindIterable(underlying.find(filter))

  def withCodecRegistry(codecRegistry: CodecRegistry): BsonAdtCollection =
    new BsonAdtCollection(underlying.withCodecRegistry(codecRegistry))

  def createIndexes(indexes: Seq[IndexModel]): Seq[String] = underlying.createIndexes(indexes).to[Array]

  def replaceOne(filter: BsonObject, replacement: BsonObject): UpdateResult = underlying.replaceOne(filter, replacement)

  def replaceOne(filter: BsonObject, replacement: BsonObject, updateOptions: UpdateOptions): UpdateResult =
    underlying.replaceOne(filter, replacement, updateOptions)

  def dropIndex(indexName: String): Unit = underlying.dropIndex(indexName)

  def dropIndex(keys: BsonObject): Unit = underlying.dropIndex(keys)

  def deleteOne(filter: BsonObject): DeleteResult = underlying.deleteOne(filter)

  def renameCollection(newCollectionNamespace: MongoNamespace): Unit =
    underlying.renameCollection(newCollectionNamespace)

  def renameCollection(newCollectionNamespace: MongoNamespace, renameCollectionOptions: RenameCollectionOptions): Unit =
    underlying.renameCollection(newCollectionNamespace, renameCollectionOptions)
}
