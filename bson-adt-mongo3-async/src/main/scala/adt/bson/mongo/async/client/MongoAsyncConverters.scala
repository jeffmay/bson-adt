package adt.bson.mongo.async.client

import adt.bson.mongo.async.MongoSingleCallback
import com.mongodb.async.SingleResultCallback
import play.api.libs.functional.syntax._

import scala.concurrent.{Future, Promise}

/**
 * Module for converting [[SingleResultCallback]]s to [[Future]]s.
 */
object MongoAsyncConverters {

  /**
   * Takes a function that requires a [[MongoSingleCallback]] and provides a callback that fulfills a promise.
   *
   * @param action a function that requires a callback
   * @return a future that is fulfilled by what is passed to the [[MongoSingleCallback]] that is provided to the action
   */
  def promise[P](action: SingleResultCallback[P] => Any): Future[P] = {
    val p = Promise[P]()
    action(MongoSingleCallback.complete(p))
    p.future
  }

  /**
   * Same as [[promise]], except that it converts the [[Void]] type into [[Unit]].
   *
   * @param action a function that requires a void callback
   * @return a future that is fulfilled with Unit when the action completes
   */
  def promiseUnit(action: SingleResultCallback[Void] => Any): Future[Unit] = {
    val p = Promise[Unit]()
    action(MongoSingleCallback.complete(p).contramap(_ => ()))
    p.future
  }
}
