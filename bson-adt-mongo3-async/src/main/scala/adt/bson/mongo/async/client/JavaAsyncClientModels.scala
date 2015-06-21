package adt.bson.mongo.async.client

object JavaAsyncClientModels extends JavaAsyncClientModels
trait JavaAsyncClientModels {

  type JavaAggregateIterable[T]       = com.mongodb.async.client.AggregateIterable[T]
  type JavaAsyncBatchCursor[T]        = com.mongodb.async.AsyncBatchCursor[T]
  type JavaDistinctIterable[T]        = com.mongodb.async.client.DistinctIterable[T]
  type JavaFindIterable[T]            = com.mongodb.async.client.FindIterable[T]
  type JavaListCollectionsIterable[T] = com.mongodb.async.client.ListCollectionsIterable[T]
  type JavaListDatabasesIterable[T]   = com.mongodb.async.client.ListDatabasesIterable[T]
  type JavaListIndexesIterable[T]     = com.mongodb.async.client.ListIndexesIterable[T]
  type JavaMapReduceIterable[T]       = com.mongodb.async.client.MapReduceIterable[T]
  type JavaMongoIterable[T]           = com.mongodb.async.client.MongoIterable[T]

  type JavaMongoClient                = com.mongodb.async.client.MongoClient
}
