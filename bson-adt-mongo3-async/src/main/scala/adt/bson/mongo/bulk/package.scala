package adt.bson.mongo

import adt.bson.BsonAdtImplicits
import adt.bson.mongo.client.result.JavaClientResults

package object bulk extends BsonAdtImplicits with JavaBulkModels with JavaClientResults
