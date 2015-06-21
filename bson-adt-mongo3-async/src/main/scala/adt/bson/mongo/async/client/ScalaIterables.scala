package adt.bson.mongo.async.client

import java.util.concurrent.TimeUnit

import adt.bson.mongo.{MongoBlock, MongoSingleCallback}
import adt.bson.{BsonJavaScript, BsonObject}
import com.mongodb.CursorType
import com.mongodb.client.model.MapReduceAction
import play.api.libs.functional.syntax._

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.convert.Wrappers.MutableSeqWrapper
import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class ScalaMongoIterable[+T](underlying: JavaMongoIterable[T]) {

  def foreach[U](block: T => U): Unit = {
    underlying.forEach(new MongoBlock(block), MongoSingleCallback.throwAnyException)
  }

  def completeForEach[U](f: T => U): Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.forEach(new MongoBlock(f), MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  def sequence: Future[Seq[T]] = {
    val target = MutableSeqWrapper(new mutable.LinkedList[T]())
    val promise = Promise[Seq[T]]()
    underlying.into(target, new MongoSingleCallback[MutableSeqWrapper[T]]({ result =>
      promise.tryComplete(result.map(_.underlying))
    }))
    promise.future
  }

  def batchSize(batchSize: Int): ScalaMongoIterable[T] = new ScalaMongoIterable(underlying.batchSize(batchSize))

  def batchCursor(): Future[ScalaAsyncBatchCursor[T]] = {
    val promise = Promise[ScalaAsyncBatchCursor[T]]()
    underlying.batchCursor(MongoSingleCallback.complete(promise).contramap(new ScalaAsyncBatchCursor(_)))
    promise.future
  }

  def firstOption: Future[Option[T]] = {
    val promise = Promise[Option[T]]()
    underlying.first(MongoSingleCallback.complete(promise).contramap(Option(_)))
    promise.future
  }

  def map[U](f: T => U): ScalaMongoIterable[U] = new ScalaMongoIterable(underlying.map(f))
}

class ScalaAsyncBatchCursor[+T](underlying: JavaAsyncBatchCursor[T]) {

  def next(): Future[Seq[T]] = {
    val promise = Promise[Seq[T]]()
    underlying.next(MongoSingleCallback.complete(promise).contramap(_.toSeq))
    promise.future
  }

  def batchSize_=(batchSize: Int): Unit = underlying.setBatchSize(batchSize)
  def batchSize: Int = underlying.getBatchSize

  def close(): Unit = underlying.close()

  def isClosed: Boolean = underlying.isClosed
}

class ScalaAggregateIterable[+T](underlying: JavaAggregateIterable[T]) extends ScalaMongoIterable(underlying) {

  def useCursor(useCursor: Boolean): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.useCursor(useCursor))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.maxTime(maxTime, timeUnit))

  def toCollection: Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.toCollection(MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  override def batchSize(batchSize: Int): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.batchSize(batchSize))

  def allowDiskUse(allowDiskUse: Boolean): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.allowDiskUse(allowDiskUse))

}

class ScalaDistinctIterable[+T](underlying: JavaDistinctIterable[T]) extends ScalaMongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaDistinctIterable[T] =
    new ScalaDistinctIterable(underlying.maxTime(maxTime, timeUnit))

  def filter(filter: BsonObject): ScalaDistinctIterable[T] = new ScalaDistinctIterable(underlying.filter(filter))
}

class ScalaFindIterable[+T](underlying: JavaFindIterable[T]) extends ScalaMongoIterable(underlying) {

  def oplogReplay(oplogReplay: Boolean): ScalaFindIterable[T] = new ScalaFindIterable(underlying.oplogReplay(oplogReplay))

  def sort(sort: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.sort(sort))

  def skip(skip: Int): ScalaFindIterable[T] = new ScalaFindIterable(underlying.skip(skip))

  def projection(projection: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.projection(projection))

  def partial(partial: Boolean): ScalaFindIterable[T] = new ScalaFindIterable(underlying.partial(partial))

  def cursorType(cursorType: CursorType): ScalaFindIterable[T] = new ScalaFindIterable(underlying.cursorType(cursorType))

  def modifiers(modifiers: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.modifiers(modifiers))

  def noCursorTimeout(noCursorTimeout: Boolean): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.noCursorTimeout(noCursorTimeout))

  def filter(filter: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.maxTime(maxTime, timeUnit))

  def limit(limit: Int): ScalaFindIterable[T] = new ScalaFindIterable(underlying.limit(limit))
}

class ScalaListIndexesIterable[+T](underlying: JavaListIndexesIterable[T]) extends ScalaMongoIterable(underlying) {

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaListIndexesIterable[T] =
    new ScalaListIndexesIterable(underlying.maxTime(maxTime, timeUnit))
}

class ScalaMapReduceIterable[+T](underlying: JavaMapReduceIterable[T]) extends ScalaMongoIterable(underlying) {

  def collectionName(collectionName: String): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.collectionName(collectionName))

  def nonAtomic(nonAtomic: Boolean): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.nonAtomic(nonAtomic))

  def databaseName(databaseName: String): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.databaseName(databaseName))

  def jsMode(jsMode: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.jsMode(jsMode))

  def scope(scope: BsonObject): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.scope(scope))

  def sort(sort: BsonObject): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.sort(sort))

  def finalizeFunction(finalizeFunction: BsonJavaScript): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.finalizeFunction(finalizeFunction.value))

  def verbose(verbose: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.verbose(verbose))

  def toCollection: Future[Unit] = {
    val promise = Promise[Unit]()
    underlying.toCollection(MongoSingleCallback.complete(promise).contramap(_ => ()))
    promise.future
  }

  def sharded(sharded: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.sharded(sharded))

  def filter(filter: BsonObject): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.maxTime(maxTime, timeUnit))

  def action(action: MapReduceAction): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.action(action))

  def limit(limit: Int): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.limit(limit))
}