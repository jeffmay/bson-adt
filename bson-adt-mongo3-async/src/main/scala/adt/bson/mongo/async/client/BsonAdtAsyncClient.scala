package adt.bson.mongo.async.client

import adt.bson.BsonObject
import adt.bson.mongo.codecs.BsonAdtCodecRegistry
import com.mongodb.ConnectionString
import com.mongodb.async.client.{MongoClient, MongoClientSettings, MongoClients}
import com.mongodb.connection._

/**
 * An asynchronous Mongo 3 client that provides [[BsonObject]] return types and scala idiomatic [[Iterable]] types
 * and [[scala.concurrent.Future]]s.
 */
class BsonAdtAsyncClient(underlying: MongoClient) {

  def settings: MongoClientSettings = underlying.getSettings

  def getDatabase(name: String): BsonAdtAsyncMongoDatabase =
    new BsonAdtAsyncMongoDatabase(underlying.getDatabase(name))

  def close(): Unit = underlying.close()

  def listDatabases(): ListDatabasesIterable[BsonObject] =
    new ListDatabasesIterable(underlying.listDatabases(classOf[BsonObject]))

  def listDatabaseNames(): MongoIterable[String] = new MongoIterable(underlying.listDatabaseNames())
}

object BsonAdtAsyncClient {

  /**
   * Connects to the given URL with the default settings.
   */
  def apply(connectionString: ConnectionString): BsonAdtAsyncClient = apply(settings(connectionString))

  /**
   * Builds a [[BsonAdtAsyncClient]] using the provided settings and the [[BsonAdtCodecRegistry]].
   *
   * This is useful for customizing non-serialization / connection settings.
   */
  def apply(settings: MongoClientSettings): BsonAdtAsyncClient = {
    val altered = MongoClientSettings.builder(settings)
      .codecRegistry(BsonAdtCodecRegistry)
    new BsonAdtAsyncClient(MongoClients.create(settings))
  }

  /**
   * Creates a connection to localhost.
   */
  def localhost(): BsonAdtAsyncClient = apply(settings())

  /**
   * Creates the default settings from the provided [[ConnectionString]] or localhost (default)
   */
  def settings(connectionString: ConnectionString = new ConnectionString("mongodb://localhost")): MongoClientSettings =
    MongoClientSettings.builder
      .codecRegistry(BsonAdtCodecRegistry)
      .clusterSettings(ClusterSettings.builder.applyConnectionString(connectionString).build)
      .connectionPoolSettings(ConnectionPoolSettings.builder.applyConnectionString(connectionString).build)
      .serverSettings(ServerSettings.builder.build).credentialList(connectionString.getCredentialList)
      .sslSettings(SslSettings.builder.applyConnectionString(connectionString).build)
      .socketSettings(SocketSettings.builder.applyConnectionString(connectionString).build)
      .build
}