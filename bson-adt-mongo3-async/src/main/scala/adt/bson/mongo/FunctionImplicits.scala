package adt.bson.mongo

import com.mongodb.async.SingleResultCallback
import play.api.libs.functional.ContravariantFunctor

import scala.concurrent.Promise
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

// TODO: Avoid this redundant trait by moving the common mongo-core-driver dependent code to a shared module
object FunctionImplicits extends FunctionImplicits
trait FunctionImplicits {

  implicit def asMongoFunction[P, R](f: P => R): com.mongodb.Function[P, R] = new MongoFunction(f)

  implicit def asBlock[R](block: R => Any): com.mongodb.Block[R] = new MongoBlock(block)
}

class MongoFunction[P, R](f: P => R) extends com.mongodb.Function[P, R] {
  override def apply(p: P): R = f(p)
}

class MongoBlock[P](f: P => Any) extends com.mongodb.Block[P] {
  override def apply(p: P): Unit = f(p)
}

object MongoSingleCallback {

  def apply[P](callback: Try[P] => Any): MongoSingleCallback[P] = new MongoSingleCallback[P](callback)

  def complete[P](promise: Promise[P]): MongoSingleCallback[P] = new MongoSingleCallback[P](x => promise.complete(x))

  @inline final def throwAnyException[T]: MongoSingleCallback[T] =
    ThrowAnyExceptionCallback.asInstanceOf[MongoSingleCallback[T]]
  object ThrowAnyExceptionCallback extends MongoSingleCallback[Any](_.get)

  implicit object MongoSingleCallbackContravariantFunctor extends ContravariantFunctor[MongoSingleCallback] {
    override def contramap[A, B](orig: MongoSingleCallback[A], preTransform: B => A): MongoSingleCallback[B] =
      new MongoSingleCallback[B](origResult => orig.callback(origResult map preTransform))
  }
}
class MongoSingleCallback[P](val callback: Try[P] => Any)
  extends SingleResultCallback[P] {

  override def onResult(result: P, e: Throwable): Unit = result match {
    case null => callback(Success(result))
    case _    => callback(Failure(e))
  }
}
