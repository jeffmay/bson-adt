package adt

package object bson extends JavaBsonValues with BsonAdtImplicits {

  type BsonDocument = BsonObject
}
