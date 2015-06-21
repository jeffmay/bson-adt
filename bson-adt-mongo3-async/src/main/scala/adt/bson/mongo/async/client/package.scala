package adt.bson.mongo.async

import adt.bson.BsonAdtImplicits
import adt.bson.mongo.bulk.JavaBulkResults

package object client extends BsonAdtImplicits with AsyncImplicits with JavaBulkResults with JavaIterables
