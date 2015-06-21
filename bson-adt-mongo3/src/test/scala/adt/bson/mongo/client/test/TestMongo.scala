package adt.bson.mongo.client.test

import adt.bson.BsonAdtImplicits
import adt.bson.mongo.client.{BsonAdtCollection, BsonClientImplicits}
import adt.bson.mongo.codecs.BsonAdtCodecProvider
import com.mongodb.client.MongoDatabase
import com.mongodb.{MongoClient, MongoClientOptions, ServerAddress}
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.{DocumentCodecProvider, ValueCodecProvider}

import scala.collection.JavaConversions.asScalaIterator
import scala.util.Try

/**
 * Provides temporary collections that are automatically cleaned up.
 */
object TestMongo extends BsonAdtImplicits with BsonClientImplicits {

  /**
   * Cache the clients, since we don't want too many of them.
   *
   * clients are async and don't provide a mechanism to await their completion, but they clean
   * up after themselves when the JVM shuts down.
   */
  private val clients = Map.empty[ServerAddress, MongoClient].withDefault(address =>
    new MongoClient(
      address,
      MongoClientOptions.builder()
        .legacyDefaults()
        .codecRegistry(CodecRegistries.fromProviders(
          BsonAdtCodecProvider,
          new DocumentCodecProvider(),
          new ValueCodecProvider))
        .build()
    )
  )

  def withClient[T](address: ServerAddress = new ServerAddress())(f: MongoClient => T): T = {
    val client = clients(address)
    f(client)
    // not closing the client here, since they should clean up automatically
  }

  def withDatabase[T](dbName: String, client: MongoClient)
    (f: MongoDatabase => T): T = {
    val fullDbName = s"${dbName}_${nextDbN(client, dbName)}"
    val db = client.getDatabase(fullDbName)
    try f(db)
    finally db.drop()
  }

  def withDatabase[T](dbName: String, address: ServerAddress = new ServerAddress())
    (f: MongoDatabase => T): T = {
    withClient(address) { client =>
      withDatabase(dbName, client) { db =>
        try f(db)
        finally db.drop()
      }
    }
  }

  def withCollection[T](name: String, db: MongoDatabase)(f: BsonAdtCollection => T): T = {
    val fullDbName = s"${name}_${nextCollectionN(db, name)}"
    val col = db.getBsonCollection(fullDbName)
    try f(col)
    finally col.drop()
  }

  /**
   * Finds the last integer suffix in the database and adds 1 to it.
   */
  private def nextDbN(mongo: MongoClient, dbName: String): Int = {
    val allDbNames = mongo.listDatabases().iterator().map(_.get("name", classOf[String])).toStream
    nextN(allDbNames, dbName)
  }

  private def nextCollectionN(db: MongoDatabase, collectionName: String): Int = {
    val allCollectionNames = db.listCollectionNames().iterator().toStream
    nextN(allCollectionNames, collectionName)
  }

  private def nextN(allNames: Seq[String], newName: String): Int = {
    val matchingNamesSorted =
      allNames.map { name =>
        if (name.startsWith(name)) {
          Try {
            val suffix = name.substring(name.length + 1)
            suffix.toInt
          }.toOption
        }
        else None
      }.collect {
        case Some(suffix) => suffix
      }.sorted(Ordering.Int.reverse)  // greatest to least
    // get the last test number or 0
    val lastTestN = matchingNamesSorted.headOption getOrElse 0
    lastTestN + 1
  }
}