package adt.bson.mongo

import scala.language.implicitConversions

// TODO: Avoid this redundant trait by moving the common mongo-core-driver dependent code to a shared module
object MongoFunctionImplicits extends MongoFunctionImplicits
trait MongoFunctionImplicits {

  implicit def asMongoFunction[P, R](f: P => R): com.mongodb.Function[P, R] = new MongoFunction(f)

  implicit def asBlock[R](block: R => Any): com.mongodb.Block[R] = new MongoBlock(block)
}

class MongoFunction[P, R](f: P => R) extends com.mongodb.Function[P, R] {
  override def apply(p: P): R = f(p)
}

class MongoBlock[P](f: P => Any) extends com.mongodb.Block[P] {
  override def apply(p: P): Unit = f(p)
}
