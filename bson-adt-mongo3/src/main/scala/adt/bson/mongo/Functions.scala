package adt.bson.mongo

class MongoFunction[P, R](f: P => R) extends com.mongodb.Function[P, R] {
  override def apply(p: P): R = f(p)
}

class MongoBlock[P](f: P => Unit) extends com.mongodb.Block[P] {
  override def apply(p: P): Unit = f(p)
}
