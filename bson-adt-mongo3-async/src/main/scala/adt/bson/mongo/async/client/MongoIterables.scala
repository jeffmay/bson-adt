package adt.bson.mongo.async.client

import java.util.concurrent.TimeUnit

import adt.bson.mongo.MongoBlock
import adt.bson.mongo.async.{AsyncBatchCursor, MongoSingleCallback}
import adt.bson.{BsonJavaScript, BsonObject}
import com.mongodb.CursorType
import com.mongodb.client.model.MapReduceAction
import play.api.libs.functional.syntax._

import scala.collection.convert.Wrappers.MutableBufferWrapper
import scala.collection.mutable
import scala.concurrent.{Future, Promise}

/**
 * Substitutes [[com.mongodb.async.client.MongoIterable]]
 */
class MongoIterable[+T](underlying: JavaMongoIterable[T]) {

  def foreach[U](block: T => U): Unit = {
    underlying.forEach(new MongoBlock(block), MongoSingleCallback.throwAnyException)
  }

  def completeForEach[U](f: T => U): Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.forEach(new MongoBlock(f), MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  def sequence(): Future[Seq[T]] = {
    val target = MutableBufferWrapper(mutable.Buffer.empty[T])
    val promise = Promise[Seq[T]]()
    underlying.into(target, new MongoSingleCallback[MutableBufferWrapper[T]]({ result =>
      promise.tryComplete(result.map(_.underlying))
    }))
    promise.future
  }

  def batchSize(batchSize: Int): MongoIterable[T] = new MongoIterable(underlying.batchSize(batchSize))

  def batchCursor(): Future[AsyncBatchCursor[T]] = {
    val promise = Promise[AsyncBatchCursor[T]]()
    underlying.batchCursor(MongoSingleCallback.complete(promise).contramap(new AsyncBatchCursor(_)))
    promise.future
  }

  def first(): Future[Option[T]] = {
    val promise = Promise[Option[T]]()
    underlying.first(MongoSingleCallback.complete(promise).contramap(Option(_)))
    promise.future
  }

  def map[U](f: T => U): MongoIterable[U] = new MongoIterable(underlying.map(f))
}

/**
 * Substitutes [[com.mongodb.async.client.AggregateIterable]]
 */
class AggregateIterable[+T](underlying: JavaAggregateIterable[T]) extends MongoIterable(underlying) {

  def useCursor(useCursor: Boolean): AggregateIterable[T] =
    new AggregateIterable(underlying.useCursor(useCursor))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): AggregateIterable[T] =
    new AggregateIterable(underlying.maxTime(maxTime, timeUnit))

  def toCollection: Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.toCollection(MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  override def batchSize(batchSize: Int): AggregateIterable[T] =
    new AggregateIterable(underlying.batchSize(batchSize))

  def allowDiskUse(allowDiskUse: Boolean): AggregateIterable[T] =
    new AggregateIterable(underlying.allowDiskUse(allowDiskUse))

}

/**
 * Substitutes [[com.mongodb.async.client.DistinctIterable]]
 */
class DistinctIterable[+T](underlying: JavaDistinctIterable[T]) extends MongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): DistinctIterable[T] =
    new DistinctIterable(underlying.maxTime(maxTime, timeUnit))

  def filter(filter: BsonObject): DistinctIterable[T] = new DistinctIterable(underlying.filter(filter))
}

/**
 * Substitutes [[com.mongodb.async.client.FindIterable]]
 */
class FindIterable[+T](underlying: JavaFindIterable[T]) extends MongoIterable(underlying) {

  def oplogReplay(oplogReplay: Boolean): FindIterable[T] = new FindIterable(underlying.oplogReplay(oplogReplay))

  def sort(sort: BsonObject): FindIterable[T] = new FindIterable(underlying.sort(sort))

  def skip(skip: Int): FindIterable[T] = new FindIterable(underlying.skip(skip))

  def projection(projection: BsonObject): FindIterable[T] = new FindIterable(underlying.projection(projection))

  def partial(partial: Boolean): FindIterable[T] = new FindIterable(underlying.partial(partial))

  def cursorType(cursorType: CursorType): FindIterable[T] = new FindIterable(underlying.cursorType(cursorType))

  def modifiers(modifiers: BsonObject): FindIterable[T] = new FindIterable(underlying.modifiers(modifiers))

  def noCursorTimeout(noCursorTimeout: Boolean): FindIterable[T] =
    new FindIterable(underlying.noCursorTimeout(noCursorTimeout))

  def filter(filter: BsonObject): FindIterable[T] = new FindIterable(underlying.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): FindIterable[T] =
    new FindIterable(underlying.maxTime(maxTime, timeUnit))

  def limit(limit: Int): FindIterable[T] = new FindIterable(underlying.limit(limit))
}

/**
 * Substitutes [[com.mongodb.async.client.ListCollectionsIterable]]
 */
class ListCollectionsIterable[+T](underlying: JavaListCollectionsIterable[T]) extends MongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ListCollectionsIterable[T] =
    new ListCollectionsIterable(underlying.maxTime(maxTime, timeUnit))

  def filter(filter: BsonObject): ListCollectionsIterable[T] = new ListCollectionsIterable(underlying.filter(filter))
}

/**
 * Substitutes [[com.mongodb.async.client.ListDatabasesIterable]]
 */
class ListDatabasesIterable[+T](underlying: JavaListDatabasesIterable[T]) extends MongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ListDatabasesIterable[T] =
    new ListDatabasesIterable(underlying.maxTime(maxTime, timeUnit))
}

/**
 * Substitutes [[com.mongodb.async.client.ListIndexesIterable]]
 */
class ListIndexesIterable[+T](underlying: JavaListIndexesIterable[T]) extends MongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ListIndexesIterable[T] =
    new ListIndexesIterable(underlying.maxTime(maxTime, timeUnit))
}

/**
 * Substitutes [[com.mongodb.async.client.MapReduceIterable]]
 */
class MapReduceIterable[+T](underlying: JavaMapReduceIterable[T]) extends MongoIterable(underlying) {

  def collectionName(collectionName: String): MapReduceIterable[T] =
    new MapReduceIterable(underlying.collectionName(collectionName))

  def nonAtomic(nonAtomic: Boolean): MapReduceIterable[T] =
    new MapReduceIterable(underlying.nonAtomic(nonAtomic))

  def databaseName(databaseName: String): MapReduceIterable[T] =
    new MapReduceIterable(underlying.databaseName(databaseName))

  def jsMode(jsMode: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.jsMode(jsMode))

  def scope(scope: BsonObject): MapReduceIterable[T] = new MapReduceIterable(underlying.scope(scope))

  def sort(sort: BsonObject): MapReduceIterable[T] = new MapReduceIterable(underlying.sort(sort))

  def finalizeFunction(finalizeFunction: BsonJavaScript): MapReduceIterable[T] =
    new MapReduceIterable(underlying.finalizeFunction(finalizeFunction.value))

  def verbose(verbose: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.verbose(verbose))

  def toCollection: Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.toCollection(MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  def sharded(sharded: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.sharded(sharded))

  def filter(filter: BsonObject): MapReduceIterable[T] = new MapReduceIterable(underlying.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): MapReduceIterable[T] =
    new MapReduceIterable(underlying.maxTime(maxTime, timeUnit))

  def action(action: MapReduceAction): MapReduceIterable[T] = new MapReduceIterable(underlying.action(action))

  def limit(limit: Int): MapReduceIterable[T] = new MapReduceIterable(underlying.limit(limit))
}