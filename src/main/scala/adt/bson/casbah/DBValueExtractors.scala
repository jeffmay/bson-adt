package adt.bson.casbah

import adt.bson._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

import scala.language.implicitConversions
import scala.util.matching.Regex

/**
 * Extracts a [[MongoDBList]] from a raw value, if possible.
 *
 * @note if attempting to extract a [[Traversable]], you should always put this extractor
 *       AFTER the [[DBObject]] extractor, as this is a little more lenient.
 *
 * [[BasicDBList]] is the only list representation in the underlying driver and it
 * extends from [[DBObject]]. The extractor will avoid checking this type.
 */
object DBList extends DBExtractor[MongoDBList] {

  override def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: MongoDBObject | _: BasicDBObject => false  // Don't match on DBObject
    case _: MongoDBList | _: BasicDBList | _: Traversable[_] | _: Array[_] => true
    case _ => false
  }

  override def unapply(value: Any): Option[MongoDBList] = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: MongoDBObject | _: BasicDBObject => None  // Don't match on DBObject
    case x: MongoDBList => Some(x)
    case x: BasicDBList => Some(x)
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Traversable[_] => Some(MongoDBList.concat(x))
    case x: Array[_] =>       Some(MongoDBList.concat(x))
    case _ => None
  }
}

/**
 * Extracts a [[MongoDBObject]] from a raw value, if possible.
 *
 * @note this will also match a Map of String, but only if this extractor is used
 *       before the [[DBList]] extractor.
 *
 * [[BasicDBObject]] is the underlying representation for Mongo objects and
 * extends from [[DBObject]]. The extractor will exploit this inheritance.
 */
object DBObject extends DBExtractor[MongoDBObject] {

  override def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: MongoDBList | _: BasicDBList => false
    case _: MongoDBObject | _: DBObject => true
    case x: Map[_, _] => x.keys forall (_.isInstanceOf[String])
    case _ => false
  }

  override def unapply(value: Any): Option[MongoDBObject] = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: MongoDBList | _: BasicDBList => None
    case x: MongoDBObject => Some(x)
    case x: DBObject =>      Some(x)  // BasicDBObject extends from DBObject, so this will match both
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Map[_, _] if x.keys forall (_.isInstanceOf[String]) =>
      val fields = x.toSeq map { case (k, v) => (k.asInstanceOf[String], v) }
      Some(MongoDBObject(fields: _*))
    case _ => None
  }
}

/**
 * Extend this to add to scope the Casbah-friendly value extractors (see [[DBExtractor]]).
 */
trait CasbahDBExtractors extends DBValueExtractors with DefaultDBValueExtractor {

  override type DBListType = MongoDBList
  override type DBObjectType = MongoDBObject

  override protected def isEmpty(dbo: MongoDBObject): Boolean = dbo.isEmpty

  override val DBList: DBExtractor[MongoDBList] = adt.bson.casbah.DBList
  override val DBObject: DBExtractor[MongoDBObject] = adt.bson.casbah.DBObject
  override val DBBinary: DBExtractor[Array[Byte]] = adt.bson.mongodb.DBBinary
  override val DBRegex: DBExtractor[Regex] = adt.bson.mongodb.DBRegex
  override val DBDate: DBExtractor[DateTime] = adt.bson.mongodb.DBDate
}

object CasbahDBExtractors extends CasbahDBExtractors
