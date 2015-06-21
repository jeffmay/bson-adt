package adt.bson.mongo.async.client

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
 * Provides temporary collections that are automatically cleaned up.
 */
object TestMongo {

  private var client: BsonAdtAsyncClient = null

  def shutdown(): Unit = {
    if (client ne null) {
      client.close()
    }
  }

  def withClient[T]()(f: BsonAdtAsyncClient => T): T = {
    if (client eq null) {
      client = BsonAdtAsyncClient.localhost()
    }
    f(client)
  }

  def withDatabase[T](dbName: String, client: BsonAdtAsyncClient)(f: BsonAdtAsyncMongoDatabase => T): T = {
    val db = synchronized {
      val fullDbName = s"${dbName}_${nextDbN(client, dbName)}"
      client.getDatabase(fullDbName)
    }
    try f(db)
    finally db.drop()
  }

  def withDatabase[T](dbName: String)(f: BsonAdtAsyncMongoDatabase => T): T = {
    withClient() { client =>
      withDatabase(dbName, client) { db =>
        try f(db)
        finally db.drop()
      }
    }
  }

  def withCollection[T](name: String, db: BsonAdtAsyncMongoDatabase)(f: BsonAdtAsyncCollection => T): T = {
    val col = synchronized {
      val fullDbName = s"${name}_${nextCollectionN(db, name)}"
      db.getCollection(fullDbName)
    }
    try f(col)
    finally col.drop()
  }


  private def nextDbN(mongo: BsonAdtAsyncClient, dbName: String): Int = {
    val allDbNames = Await.result(mongo.listDatabaseNames().sequence(), 5.seconds)
    nextN(allDbNames, dbName)
  }

  private def nextCollectionN(db: BsonAdtAsyncMongoDatabase, collectionName: String): Int = {
    val allCollectionNames = Await.result(db.listCollectionNames().sequence(), 5.seconds)
    nextN(allCollectionNames, collectionName)
  }

  /**
   * Finds the last integer suffix of the given names and adds 1 to it.
   */
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