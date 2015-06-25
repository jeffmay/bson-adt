package adt.bson.mongo.client.result

object JavaClientResults extends JavaClientResults
trait JavaClientResults {
  type JavaUpdateResult = com.mongodb.client.result.UpdateResult
  type JavaDeleteResult = com.mongodb.client.result.DeleteResult
}

