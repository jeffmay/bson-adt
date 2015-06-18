package adt.bson.mongo.client

import adt.bson.BsonObject
import adt.bson.mongo.client.BsonClientImplicits.BsonDatabaseOps
import com.mongodb.client.MongoDatabase

import scala.language.implicitConversions

trait BsonClientImplicits {

  implicit def toBsonDatabaseOps(db: MongoDatabase): BsonDatabaseOps = new BsonDatabaseOps(db)
}

object BsonClientImplicits {

  class BsonDatabaseOps(val db: MongoDatabase) extends AnyVal {

    def getBsonCollection(name: String): BsonAdtCollection =
      new BsonAdtCollection(db.getCollection(name, classOf[BsonObject]))
  }
}
