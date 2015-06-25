package adt.bson.mongo.async

import adt.bson.mongo.async.client._
import play.api.libs.functional.syntax._

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.concurrent.{Future, Promise}

/**
 * Substitutes [[com.mongodb.async.AsyncBatchCursor]]
 */
class AsyncBatchCursor[+T](underlying: JavaAsyncBatchCursor[T]) {

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
