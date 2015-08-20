package adt.bson.mongo.client

import adt.bson.mongo.codecs.BsonAdtCodecRegistry
import com.mongodb.{MongoClient, MongoClientOptions, ServerAddress}

import scala.collection.JavaConversions.asScalaIterator
import scala.util.Try

/**
 * Provides temporary collections that are automatically cleaned up.
 */
object TestMongo {

  /**
   * Cleaned up by [[adt.bson.test.Cleanup]]
   */
  private var client: MongoClient = null

  def shutdown(): Unit = {
    if (client ne null) {
      client.close()
    }
  }

  def withClient[T]()(f: MongoClient => T): T = {
    if (client eq null) {
      client = new MongoClient(
        new ServerAddress(),
        MongoClientOptions.builder()
          .codecRegistry(BsonAdtCodecRegistry)
          .build()
      )
    }
    f(client)
  }

  def withDatabase[T](dbName: String, client: MongoClient)(f: BsonAdtDatabase => T): T = {
    val db = synchronized {
      val fullDbName = s"${dbName}_${nextDbN(client, dbName)}"
      new BsonAdtDatabase(client.getDatabase(fullDbName))
    }
    try f(db)
    finally db.drop()
  }

  def withDatabase[T](dbName: String)(f: BsonAdtDatabase => T): T = {
    withClient() { client =>
      withDatabase(dbName, client) { db =>
        f(db)
      }
    }
  }

  def withCollection[T](name: String, db: BsonAdtDatabase)(f: BsonAdtCollection => T): T = {
    val col = synchronized {
      val fullDbName = s"${name}_${nextCollectionN(db, name)}"
      db.getCollection(fullDbName)
    }
    try f(col)
    finally col.drop()
  }

  private def nextDbN(mongo: MongoClient, dbName: String): Int = {
    val allDbNames = mongo.listDatabases().iterator().map(_.get("name", classOf[String])).toStream
    nextN(allDbNames, dbName)
  }

  private def nextCollectionN(db: BsonAdtDatabase, collectionName: String): Int = {
    val allCollectionNames = db.listCollectionNames().toStream
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