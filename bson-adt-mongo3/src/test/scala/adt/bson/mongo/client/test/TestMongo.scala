package adt.bson.mongo.client.test

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
object TestMongo {

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

  def withClient(address: ServerAddress = new ServerAddress())(f: MongoClient => Unit): Unit = {
    val client = clients(address)
    f(client)
  }

  def withDatabase(dbName: String, client: MongoClient)
    (f: MongoDatabase => Unit): Unit = {
    val fullDbName = s"${dbName}_${nextN(client, dbName)}"
    val db = client.getDatabase(fullDbName)
    try f(db)
    finally db.drop()
  }

  def withDatabase(dbName: String, address: ServerAddress = new ServerAddress())
    (f: MongoDatabase => Unit): Unit = {
    withClient(address) { client =>
      withDatabase(dbName, client) { db =>
        f(db)
      }
    }
  }

  /**
   * Finds the last integer suffix in the database and adds 1 to it.
   */
  private def nextN(mongo: MongoClient, dbName: String): Int = {
    val dbs = mongo.listDatabases().iterator().toStream
    val prevDbSuffixes =
      dbs.map { doc =>
        val name = doc.get("name", classOf[String])
        if (name.startsWith(dbName)) {
          Try {
            val suffix = name.substring(dbName.length + 1)
            suffix.toInt
          }.toOption
        }
        else None
      }.collect {
        case Some(suffix) => suffix
      }.sorted(Ordering.Int.reverse)  // greatest to least
    // get the last test number or 0
    val lastTestN = prevDbSuffixes.headOption getOrElse 0
    lastTestN + 1
  }
}