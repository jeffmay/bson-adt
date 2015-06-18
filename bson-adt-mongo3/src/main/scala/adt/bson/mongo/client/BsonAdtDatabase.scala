package adt.bson.mongo.client

import adt.bson.BsonObject
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.{ReadPreference, WriteConcern}
import org.bson.codecs.configuration.CodecRegistry

/**
 * Wraps the [[JavaMongoDatabase]] to provide methods that return [[BsonObject]].
 *
 * Substitutes [[com.mongodb.client.MongoDatabase]].
 */
class BsonAdtDatabase(underlying: JavaMongoDatabase) {
  // TODO: Validate CodecRegistry has BsonObject

  def getCollection(collectionName: String): BsonAdtCollection =
    new BsonAdtCollection(underlying.getCollection(collectionName, classOf[BsonObject]))

  def listCollections(): ListCollectionsIterable[BsonObject] =
    new ListCollectionsIterable(underlying.listCollections(classOf[BsonObject]))

  def createCollection(collectionName: String): Unit = underlying.createCollection(collectionName)

  def createCollection(collectionName: String, createCollectionOptions: CreateCollectionOptions): Unit =
    underlying.createCollection(collectionName, createCollectionOptions)

  def listCollectionNames(): MongoIterable[String] = new MongoIterable(underlying.listCollectionNames())

  def name: String = underlying.getName

  def runCommand(command: BsonObject): BsonObject = underlying.runCommand(command, classOf[BsonObject])

  def runCommand(command: BsonObject, readPreference: ReadPreference): BsonObject =
    underlying.runCommand(command, readPreference, classOf[BsonObject])

  def drop(): Unit = underlying.drop()

  def withReadPreference(readPreference: ReadPreference): BsonAdtDatabase =
    new BsonAdtDatabase(underlying.withReadPreference(readPreference))

  def withWriteConcern(writeConcern: WriteConcern): BsonAdtDatabase =
    new BsonAdtDatabase(underlying.withWriteConcern(writeConcern))

  // TODO: Validate CodecRegistry has BsonObject
  def withCodecRegistry(codecRegistry: CodecRegistry): BsonAdtDatabase =
    new BsonAdtDatabase(underlying.withCodecRegistry(codecRegistry))

  def codecRegistry: CodecRegistry = underlying.getCodecRegistry

  def readPreference: ReadPreference = underlying.getReadPreference

  def writeConcern: WriteConcern = underlying.getWriteConcern
}
