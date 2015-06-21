package adt.bson.mongo.async

import adt.bson.mongo.{FunctionImplicits, MongoSingleCallback}
import com.mongodb.async.SingleResultCallback

import scala.language.implicitConversions
import scala.util.Try

trait AsyncImplicits extends FunctionImplicits {

  implicit def asSingleResultCallback[P](body: Try[P] => Any): SingleResultCallback[P] = new MongoSingleCallback(body)
}

