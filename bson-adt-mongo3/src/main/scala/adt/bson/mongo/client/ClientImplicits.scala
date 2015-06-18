package adt.bson.mongo.client

import adt.bson.BsonObject
import com.mongodb.client.MongoDatabase

import scala.language.implicitConversions

trait ClientImplicits {

  implicit def toBsonDatabaseOps(db: MongoDatabase): BsonDatabaseOps = new BsonDatabaseOps(db)
}

class BsonDatabaseOps(val db: MongoDatabase) extends AnyVal {

  def getBsonCollection(name: String): BsonAdtCollection =
    new BsonAdtCollection(db.getCollection(name, classOf[BsonObject]))
}
