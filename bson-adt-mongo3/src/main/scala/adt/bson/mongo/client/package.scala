package adt.bson.mongo

import adt.bson.BsonAdtImplicits

package object client extends BsonAdtImplicits with BsonClientImplicits with FunctionImplicits with JavaWriteModels
