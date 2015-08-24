package adt

package object bson extends JavaBsonValues with BsonAdtImplicits {

  type BsonNumber = BsonDouble
  type BsonDocument = BsonObject
}
