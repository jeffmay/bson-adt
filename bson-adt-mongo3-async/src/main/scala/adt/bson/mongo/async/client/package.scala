package adt.bson.mongo.async

import adt.bson.BsonAdtImplicits
import adt.bson.mongo.bulk.JavaBulkModels

package object client
  extends BsonAdtImplicits
  with MongoAsyncImplicits
  with JavaBulkModels
  with JavaAsyncClientModels
