package adt.bson.mongo.client

import adt.bson.mongo.MongoBlock
import adt.bson.mongo.client.ScalaMongoCursor.ServerOps
import adt.bson.{BsonJavaScript, BsonObject}
import com.mongodb._
import com.mongodb.client._
import com.mongodb.client.model.MapReduceAction
import org.bson.conversions.Bson

import scala.collection.convert.Wrappers.IterableWrapper
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.higherKinds

class ScalaAggregateIterable[T](underlying: AggregateIterable[T])
  extends ScalaMongoIterable(underlying)
  with ScalaMongoIterableMaxTime[T] {
  override type IterableType = ScalaAggregateIterable[T]

  def useCursor(useCursor: Boolean): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.useCursor(useCursor))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.maxTime(maxTime, timeUnit))

  override def batchSize(batchSize: Int): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.batchSize(batchSize))

  def allowDiskUse(allowDiskUse: Boolean): ScalaAggregateIterable[T] =
    new ScalaAggregateIterable(underlying.allowDiskUse(allowDiskUse))
}

class ScalaDistinctIterable[T](underlying: DistinctIterable[T])
  extends ScalaMongoIterable(underlying)
  with ScalaMongoIterableFilter[T]
  with ScalaMongoIterableMaxTime[T] {
  override type IterableType = ScalaDistinctIterable[T]

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaDistinctIterable[T] =
    new ScalaDistinctIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): ScalaDistinctIterable[T] =
    new ScalaDistinctIterable(underlying.filter(filter))
}

class ScalaFindIterable[T](underlying: FindIterable[T])
  extends ScalaMongoIterable(underlying)
  with SortFilterLimitMaxTime[T]
  with ScalaMongoIterableSort[T] {
  override type IterableType = ScalaFindIterable[T]

  override def limit(limit: Int): ScalaFindIterable[T] = new ScalaFindIterable(underlying.limit(limit))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.filter(filter))

  def noCursorTimeout(noCursorTimeout: Boolean): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.noCursorTimeout(noCursorTimeout))

  def modifiers(modifiers: BsonObject): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.modifiers(modifiers))

  def cursorType(cursorType: CursorType): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.cursorType(cursorType))

  def partial(partial: Boolean): ScalaFindIterable[T] = new ScalaFindIterable(underlying.partial(partial))

  def projection(projection: Bson): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.projection(projection))

  def skip(skip: Int): ScalaFindIterable[T] = new ScalaFindIterable(underlying.skip(skip))

  override def sort(sort: BsonObject): ScalaFindIterable[T] = new ScalaFindIterable(underlying.sort(sort))

  def oplogReplay(oplogReplay: Boolean): ScalaFindIterable[T] =
    new ScalaFindIterable(underlying.oplogReplay(oplogReplay))
}

class ScalaListIndexesIterable[T](underlying: ListIndexesIterable[T])
  extends ScalaMongoIterable(underlying)
  with ScalaMongoIterableMaxTime[T] {
  override type IterableType = ScalaListIndexesIterable[T]

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaListIndexesIterable[T] =
    new ScalaListIndexesIterable(underlying.maxTime(maxTime, timeUnit))
}

class ScalaMapReduceIterable[T](underlying: MapReduceIterable[T])
  extends ScalaMongoIterable(underlying)
  with SortFilterLimitMaxTime[T] {
  override type IterableType = ScalaMapReduceIterable[T]

  override def limit(limit: Int): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.limit(limit))

  def action(action: MapReduceAction): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.action(action))

  override def maxTime(maxTime: Long, timeUnit: TimeUnit): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.maxTime(maxTime, timeUnit))

  override def filter(filter: BsonObject): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.filter(filter))

  def sharded(sharded: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.sharded(sharded))

  def verbose(verbose: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.verbose(verbose))

  def finalizeFunction(finalizeFunction: BsonJavaScript): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.finalizeFunction(finalizeFunction.value))

  override def sort(sort: BsonObject): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.sort(sort))

  def scope(scope: BsonObject): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.scope(scope))

  def jsMode(jsMode: Boolean): ScalaMapReduceIterable[T] = new ScalaMapReduceIterable(underlying.jsMode(jsMode))

  def databaseName(databaseName: String): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.databaseName(databaseName))

  def nonAtomic(nonAtomic: Boolean): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.nonAtomic(nonAtomic))

  def collectionName(collectionName: String): ScalaMapReduceIterable[T] =
    new ScalaMapReduceIterable(underlying.collectionName(collectionName))
}

class ScalaMongoCursor[T](underlying: MongoCursor[T]) extends Iterator[T] {

  override def next(): T = underlying.next()

  override def hasNext: Boolean = underlying.hasNext

  def server: ServerOps = new ServerOps(underlying)

  def close(): Unit = underlying.close()

  def remove(): Unit = underlying.remove()
}

object ScalaMongoCursor {

  class ServerOps(val underlying: MongoCursor[_]) extends AnyVal {

    @throws[IllegalStateException]("If the Cursor is closed")
    def address: ServerAddress = underlying.getServerAddress

    @throws[IllegalStateException]("If the Cursor is closed")
    def cursor: ServerCursor = underlying.getServerCursor
  }
}

class ScalaMongoIterable[T](underlying: MongoIterable[T]) {

  def foreach[U](block: T => U): Unit = {
    val execute = new MongoBlock[T](p => block(p))
    underlying forEach execute
  }

  def batchSize(batchSize: Int): ScalaMongoIterable[T] = new ScalaMongoIterable(underlying.batchSize(batchSize))

  /**
   * Executes the underlying query and returns an iterator.
   */
  def cursor(): ScalaMongoCursor[T] = new ScalaMongoCursor[T](underlying.iterator())

  def firstOption: Option[T] = Option(underlying.first())

  def map[U](f: T => U): ScalaMongoIterable[U] = new ScalaMongoIterable(underlying map f)

  def to[C[_]](implicit bf: CanBuildFrom[Iterable[_], T, C[T]]): C[T] = {
    val list = new mutable.LinkedList[T]()
    underlying.into(IterableWrapper(list))
    list.to[C]
  }

  def toIterable: Iterable[T] = new MongoIterableWrapper[T](cursor())
}

class MongoIterableWrapper[T](override val iterator: ScalaMongoCursor[T]) extends Iterable[T]

sealed trait SortFilterLimitMaxTime[T]
  extends ScalaMongoIterableSort[T]
  with ScalaMongoIterableFilter[T]
  with ScalaMongoIterableLimit[T]
  with ScalaMongoIterableMaxTime[T]

sealed trait ScalaMongoIterableOps[T] extends ScalaMongoIterable[T] {
  type IterableType <: ScalaMongoIterable[T]
}

sealed trait ScalaMongoIterableFilter[T] extends ScalaMongoIterableOps[T] {
  def filter(filter: BsonObject): IterableType
}

sealed trait ScalaMongoIterableLimit[T] extends ScalaMongoIterableOps[T] {
  self: ScalaMongoIterable[T] =>
  def limit(limit: Int): IterableType
}

sealed trait ScalaMongoIterableMaxTime[T] extends ScalaMongoIterableOps[T] {
  self: ScalaMongoIterable[T] =>
  def maxTime(maxTime: Long, timeUnit: TimeUnit): IterableType
  @inline final def maxTime(duration: FiniteDuration): IterableType = maxTime(duration.length, duration.unit)
}

sealed trait ScalaMongoIterableSort[T] extends ScalaMongoIterableOps[T] {
  self: ScalaMongoIterable[T] =>
  def sort(sort: BsonObject): IterableType
}
