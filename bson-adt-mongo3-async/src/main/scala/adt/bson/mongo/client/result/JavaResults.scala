package adt.bson.mongo.client.result

object JavaResults extends JavaResults
trait JavaResults {
  type JavaUpdateResult = com.mongodb.client.result.UpdateResult
  type JavaDeleteResult = com.mongodb.client.result.DeleteResult
}

