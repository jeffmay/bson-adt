package adt.bson.mongo.async

import adt.bson.mongo.MongoFunctionImplicits
import com.mongodb.async.SingleResultCallback
import play.api.libs.functional.ContravariantFunctor

import scala.concurrent.Promise
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
 * Provides an implicit conversion from a Scala function to a [[SingleResultCallback]] for Mongo.
 */
trait MongoAsyncImplicits extends MongoFunctionImplicits {

  implicit def asSingleResultCallback[P](body: Try[P] => Any): SingleResultCallback[P] = new MongoSingleCallback(body)
}

/**
 * A [[SingleResultCallback]] for Mongo that has a bit more Scala idiomatic syntax and functionality.
 *
 * @note the value inside of the Try will be null if the Mongo driver is expecting a callback of [[Void]].
 *
 * @param callback the underyling scala function to call.
 */
class MongoSingleCallback[P](val callback: Try[P] => Any) extends SingleResultCallback[P] {

  /**
   * Called when the operation completes.
   * @param result the result, which may be null.  Always null if e is not null.
   * @param e      the throwable, or null if the operation completed normally
   */
  override def onResult(result: P, e: Throwable): Unit = e match {
    case null  => callback(Success(result))
    case error => callback(Failure(error))
  }
}

object MongoSingleCallback {

  def apply[P](callback: Try[P] => Any): MongoSingleCallback[P] = new MongoSingleCallback[P](callback)

  /**
   * Returns a callback function that completes the given promise when called.
   */
  def complete[P](promise: Promise[P]): MongoSingleCallback[P] = new MongoSingleCallback[P](x => promise.complete(x))

  /**
   * A callback that just succeeds or throws any exception it gets.
   */
  @inline final def throwAnyException[T]: MongoSingleCallback[T] =
    ThrowAnyExceptionCallback.asInstanceOf[MongoSingleCallback[T]]
  object ThrowAnyExceptionCallback extends MongoSingleCallback[Any](_.get)

  /**
   * When you import _ from [[play.api.libs.functional.syntax]], you get the ability to call contramap, which
   * applies the given transform function before calling the original callback function.
   */
  implicit object MongoSingleCallbackContravariantFunctor extends ContravariantFunctor[MongoSingleCallback] {
    override def contramap[A, B](orig: MongoSingleCallback[A], preTransform: B => A): MongoSingleCallback[B] =
      new MongoSingleCallback[B](origResult =>
        orig.callback(origResult map preTransform)
      )
  }
}
