package adt.bson.mongo.client

object JavaClientTypes extends JavaClientTypes
trait JavaClientTypes {
  type JavaAggregateIterable[T] = com.mongodb.client.AggregateIterable[T]
  type JavaDistinctIterable[T] = com.mongodb.client.DistinctIterable[T]
  type JavaFindIterable[T] = com.mongodb.client.FindIterable[T]
  type JavaListCollectionsIterable[T] = com.mongodb.client.ListCollectionsIterable[T]
  type JavaListDatabasesIterable[T] = com.mongodb.client.ListDatabasesIterable[T]
  type JavaListIndexesIterable[T] = com.mongodb.client.ListIndexesIterable[T]
  type JavaMapReduceIterable[T] = com.mongodb.client.MapReduceIterable[T]
  type JavaMongoCursor[T] = com.mongodb.client.MongoCursor[T]
  type JavaMongoIterable[T] = com.mongodb.client.MongoIterable[T]

  type JavaMongoDatabase = com.mongodb.client.MongoDatabase
}
