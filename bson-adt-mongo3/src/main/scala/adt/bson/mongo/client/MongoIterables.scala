package adt.bson.mongo.client

import adt.bson.mongo.client.MongoCursor.ServerOps
import adt.bson.{BsonJavaScript, BsonObject}
import com.mongodb._
import com.mongodb.client.model.MapReduceAction
import org.bson.conversions.Bson

import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.concurrent.blocking
import scala.concurrent.duration._

/**
 * Substitutes [[com.mongodb.client.AggregateIterable]].
 */
class AggregateIterable[T](underlying: JavaAggregateIterable[T])
  extends MongoIterable(underlying)
  with MongoIterableMaxTime[T] {
  override type IterableType = AggregateIterable[T]

  def useCursor(useCursor: Boolean): AggregateIterable[T] = new AggregateIterable(underlying.useCursor(useCursor))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): AggregateIterable[T] =
    new AggregateIterable(underlying.maxTime(maxTime, timeUnit))

  override def batchSize(batchSize: Int): AggregateIterable[T] = new AggregateIterable(underlying.batchSize(batchSize))

  def allowDiskUse(allowDiskUse: Boolean): AggregateIterable[T] =
    new AggregateIterable(underlying.allowDiskUse(allowDiskUse))
}

/**
 * Substitutes [[com.mongodb.client.DistinctIterable]].
 */
class DistinctIterable[T](underlying: JavaDistinctIterable[T])
  extends MongoIterable(underlying)
  with MongoIterableFilter[T]
  with MongoIterableMaxTime[T] {
  override type IterableType = DistinctIterable[T]

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): DistinctIterable[T] =
    new DistinctIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): DistinctIterable[T] = new DistinctIterable(underlying.filter(filter))
}

/**
 * Substitutes [[com.mongodb.client.FindIterable]].
 */
class FindIterable[T](underlying: JavaFindIterable[T])
  extends MongoIterable(underlying)
  with SortFilterLimitMaxTime[T]
  with MongoIterableSort[T] {
  override type IterableType = FindIterable[T]

  override def limit(limit: Int): FindIterable[T] = new FindIterable(underlying.limit(limit))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): FindIterable[T] =
    new FindIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): FindIterable[T] = new FindIterable(underlying.filter(filter))

  def noCursorTimeout(noCursorTimeout: Boolean): FindIterable[T] =
    new FindIterable(underlying.noCursorTimeout(noCursorTimeout))

  def modifiers(modifiers: BsonObject): FindIterable[T] = new FindIterable(underlying.modifiers(modifiers))

  def cursorType(cursorType: CursorType): FindIterable[T] = new FindIterable(underlying.cursorType(cursorType))

  def partial(partial: Boolean): FindIterable[T] = new FindIterable(underlying.partial(partial))

  def projection(projection: Bson): FindIterable[T] = new FindIterable(underlying.projection(projection))

  def skip(skip: Int): FindIterable[T] = new FindIterable(underlying.skip(skip))

  override def sort(sort: BsonObject): FindIterable[T] = new FindIterable(underlying.sort(sort))

  def oplogReplay(oplogReplay: Boolean): FindIterable[T] = new FindIterable(underlying.oplogReplay(oplogReplay))
}

class ListCollectionsIterable[T](underlying: JavaListCollectionsIterable[T])
  extends MongoIterable(underlying)
  with MongoIterableFilter[T]
  with MongoIterableMaxTime[T] {
  override type IterableType = ListCollectionsIterable[T]

  override def batchSize(batchSize: Int): ListCollectionsIterable[T] =
    new ListCollectionsIterable(underlying.batchSize(batchSize))

  def filter(filter: BsonObject): ListCollectionsIterable[T] = new ListCollectionsIterable(underlying.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ListCollectionsIterable[T] =
    new ListCollectionsIterable(underlying.maxTime(maxTime, timeUnit))
}

/**
 * Substitutes [[com.mongodb.client.ListIndexesIterable]].
 */
class ListIndexesIterable[T](underlying: JavaListIndexesIterable[T])
  extends MongoIterable(underlying)
  with MongoIterableMaxTime[T] {
  override type IterableType = ListIndexesIterable[T]

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ListIndexesIterable[T] =
    new ListIndexesIterable(underlying.maxTime(maxTime, timeUnit))
}

/**
 * Substitutes [[com.mongodb.client.MapReduceIterable]]
 */
class MapReduceIterable[T](underlying: JavaMapReduceIterable[T])
  extends MongoIterable(underlying)
  with SortFilterLimitMaxTime[T] {
  override type IterableType = MapReduceIterable[T]

  override def limit(limit: Int): MapReduceIterable[T] = new MapReduceIterable(underlying.limit(limit))

  def action(action: MapReduceAction): MapReduceIterable[T] = new MapReduceIterable(underlying.action(action))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): MapReduceIterable[T] =
    new MapReduceIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): MapReduceIterable[T] =
    new MapReduceIterable(underlying.filter(filter))

  def sharded(sharded: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.sharded(sharded))

  def verbose(verbose: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.verbose(verbose))

  def finalizeFunction(finalizeFunction: BsonJavaScript): MapReduceIterable[T] =
    new MapReduceIterable(underlying.finalizeFunction(finalizeFunction.value))

  override def sort(sort: BsonObject): MapReduceIterable[T] = new MapReduceIterable(underlying.sort(sort))

  def scope(scope: BsonObject): MapReduceIterable[T] = new MapReduceIterable(underlying.scope(scope))

  def jsMode(jsMode: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.jsMode(jsMode))

  def databaseName(databaseName: String): MapReduceIterable[T] =
    new MapReduceIterable(underlying.databaseName(databaseName))

  def nonAtomic(nonAtomic: Boolean): MapReduceIterable[T] = new MapReduceIterable(underlying.nonAtomic(nonAtomic))

  def collectionName(collectionName: String): MapReduceIterable[T] =
    new MapReduceIterable(underlying.collectionName(collectionName))
}

/**
 * Substitutes [[com.mongodb.client.MongoCursor]]
 */
class MongoCursor[T](underlying: JavaMongoCursor[T]) extends Iterator[T] {

  @throws[IllegalStateException]("If the Cursor is closed")
  override def next(): T = underlying.next()

  @throws[IllegalStateException]("If the Cursor is closed")
  override def hasNext: Boolean = underlying.hasNext

  def server: ServerOps = new ServerOps(underlying)

  def close(): Unit = underlying.close()
}
object MongoCursor {

  class ServerOps(val underlying: JavaMongoCursor[_]) extends AnyVal {

    @throws[IllegalStateException]("If the Cursor is closed")
    def address: ServerAddress = underlying.getServerAddress

    @throws[IllegalStateException]("If the Cursor is closed")
    def cursor: ServerCursor = underlying.getServerCursor
  }
}

/**
 * Substitutes [[com.mongodb.client.MongoIterable]]
 */
class MongoIterable[T](underlying: JavaMongoIterable[T]) extends Iterable[T] {

  def batchSize(batchSize: Int): MongoIterable[T] = new MongoIterable(underlying.batchSize(batchSize))

  /**
   * Executes the underlying query and returns an iterator.
   */
  def cursor(): MongoCursor[T] = new MongoCursor[T](underlying.iterator())

  /**
   * Overrides the underlying to operation so that all collection conversions happen in a blocking context
   * since the driver is lazily fetching elements.
   *
   * This covers most [[Traversable]] operations, however any post-processing (such as groupBy or collect
   * callbacks) happens outside the blocking context on the calling thread.
   */
  override def foreach[U](f: (T) => U): Unit = blocking(super.foreach(f))

  // the iterator provided is blocking, so let's put all iterator-based operations into a blocking context
  override def forall(p: T => Boolean): Boolean = blocking(super.forall(p))
  override def exists(p: T => Boolean): Boolean = blocking(super.exists(p))
  override def find(p: T => Boolean): Option[T] = blocking(super.find(p))
  override def isEmpty: Boolean = blocking(super.isEmpty)
  override def foldRight[U](z: U)(op: (T, U) => U): U = blocking(super.foldRight(z)(op))
  override def reduceRight[U >: T](op: (T, U) => U): U = blocking(super.reduceRight(op))
  override def zip[A1 >: T, B, That](that: GenIterable[B])(implicit bf: CanBuildFrom[Iterable[T], (A1, B), That]): That =
    blocking(super.zip(that))
  override def zipAll[B, A1 >: T, That](that: GenIterable[B], thisElem: A1, thatElem: B)
    (implicit bf: CanBuildFrom[Iterable[T], (A1, B), That]): That = blocking(super.zipAll(that, thisElem, thatElem))

  // executes the query and puts the results into a blocking cursor iterator
  override def iterator: Iterator[T] = cursor()
}

/**
 * A base type for interface inheritance of handy [[MongoIterable]] operations.
 */
sealed trait MongoIterableOps[T] extends MongoIterable[T] {
  type IterableType <: MongoIterable[T]
}

sealed trait MongoIterableFilter[T] extends MongoIterableOps[T] {
  def filter(filter: BsonObject): IterableType
}

sealed trait MongoIterableLimit[T] extends MongoIterableOps[T] {
  self: MongoIterable[T] =>
  def limit(limit: Int): IterableType
}

sealed trait MongoIterableMaxTime[T] extends MongoIterableOps[T] {
  self: MongoIterable[T] =>
  def maxTime(maxTime: Long, timeUnit: TimeUnit): IterableType
  @inline final def maxTime(duration: FiniteDuration): IterableType = maxTime(duration.length, duration.unit)
}

sealed trait MongoIterableSort[T] extends MongoIterableOps[T] {
  self: MongoIterable[T] =>
  def sort(sort: BsonObject): IterableType
}

sealed trait SortFilterLimitMaxTime[T]
  extends MongoIterableSort[T]
  with MongoIterableFilter[T]
  with MongoIterableLimit[T]
  with MongoIterableMaxTime[T]
