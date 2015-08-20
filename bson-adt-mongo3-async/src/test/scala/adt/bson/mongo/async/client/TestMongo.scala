package adt.bson.mongo.async.client

import scala.concurrent.{Await, blocking}
import scala.concurrent.duration._
import scala.util.Try

/**
 * Provides temporary collections that are automatically cleaned up.
 */
object TestMongo {

  private val defaultTimeout = 5.seconds

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

  def withDatabase[T](dbName: String, client: BsonAdtAsyncClient, timeout: FiniteDuration = defaultTimeout)
    (f: BsonAdtAsyncMongoDatabase => T): T = {
    val db = synchronized {
      blocking {
        val fullDbName = s"${dbName}_${nextDbN(client, dbName, timeout)}"
        val db = client.getDatabase(fullDbName)
        db
      }
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

  def withCollection[T](name: String, db: BsonAdtAsyncMongoDatabase, timeout: FiniteDuration = defaultTimeout)
    (f: BsonAdtAsyncCollection => T): T = {
    val col = synchronized {
      blocking {
        val fullDbName = s"${name}_${nextCollectionN(db, name, timeout)}"
        val col = db.getCollection(fullDbName)
        val docs = Await.result(col.find().sequence(), timeout)
        require(docs.isEmpty, s"${col.namespace} has pre-existing documents '$name' must be unique within the database")
        col
      }
    }
    try f(col)
    finally col.drop()
  }


  private def nextDbN(mongo: BsonAdtAsyncClient, dbName: String, timeout: FiniteDuration): Int = {
    val allDbNames = Await.result(mongo.listDatabaseNames().sequence(), timeout)
    nextN(allDbNames, dbName)
  }

  private def nextCollectionN(db: BsonAdtAsyncMongoDatabase, collectionName: String, timeout: FiniteDuration): Int = {
    val allCollectionNames = Await.result(db.listCollectionNames().sequence(), timeout)
    nextN(allCollectionNames, collectionName)
  }

  /**
   * Finds the last integer suffix of the given names and adds 1 to it.
   */
  private def nextN(allNames: Seq[String], newName: String): Int = {
    val matchingNamesSorted =
      allNames.map { name =>
        if (name.startsWith(newName)) {
          Try {
            val suffix = name.substring(newName.length + 1)
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