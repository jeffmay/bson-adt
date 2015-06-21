package adt.bson.mongo.async.client

import adt.bson.BsonObject
import adt.bson.mongo.async.client.MongoAsyncConverters._
import com.mongodb.async.client.MongoDatabase
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.{ReadPreference, WriteConcern}
import org.bson.codecs.configuration.CodecRegistry

import scala.concurrent.Future

class BsonAdtAsyncMongoDatabase(underlying: MongoDatabase) {

  def getCollection(collectionName: String): BsonAdtAsyncCollection =
    new BsonAdtAsyncCollection(underlying.getCollection(collectionName, classOf[BsonObject]))

  def listCollections(): ListCollectionsIterable[BsonObject] =
    new ListCollectionsIterable(underlying.listCollections(classOf[BsonObject]))

  def createCollection(collectionName: String): Future[Unit] =
    promiseUnit(underlying.createCollection(collectionName, _))

  def createCollection(collectionName: String, options: CreateCollectionOptions): Future[Unit] =
    promiseUnit(underlying.createCollection(collectionName, options, _))

  def name: String = underlying.getName

  def runCommand(command: BsonObject): Future[BsonObject] =
    promise[BsonObject](underlying.runCommand(command, classOf[BsonObject], _))

  def runCommand(command: BsonObject, readPreference: ReadPreference): Future[BsonObject] =
    promise[BsonObject](underlying.runCommand(command, readPreference, classOf[BsonObject], _))

  def codecRegistry: CodecRegistry = underlying.getCodecRegistry

  def withReadPreference(readPreference: ReadPreference): BsonAdtAsyncMongoDatabase =
    new BsonAdtAsyncMongoDatabase(underlying.withReadPreference(readPreference))

  def writeConcern: WriteConcern = underlying.getWriteConcern

  def drop(): Future[Unit] = promiseUnit(underlying.drop)

  def listCollectionNames(): MongoIterable[String] = new MongoIterable(underlying.listCollectionNames())

  def readPreference: ReadPreference = underlying.getReadPreference

  def withWriteConcern(writeConcern: WriteConcern): BsonAdtAsyncMongoDatabase =
    new BsonAdtAsyncMongoDatabase(underlying.withWriteConcern(writeConcern))

  def withCodecRegistry(codecRegistry: CodecRegistry): BsonAdtAsyncMongoDatabase =
    new BsonAdtAsyncMongoDatabase(underlying.withCodecRegistry(codecRegistry))
}
