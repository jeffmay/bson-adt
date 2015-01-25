package adt.bson.mongodb

import adt.bson._
import com.mongodb.{BasicDBList, BasicDBObject}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._
import scala.util.matching.Regex

// TODO: Support Java collections?

/**
 * Extracts a [[com.mongodb.BasicDBList]] from a mongodb or raw Scala Traversable, if possible.
 *
 * @note if attempting to extract a [[Traversable]], you should always put this extractor
 *       AFTER the [[com.mongodb.DBObject]] extractor, as this is a little more lenient.
 *
 * [[com.mongodb.BasicDBList]] is the only list representation in the underlying driver
 * and it also extends from [[com.mongodb.DBObject]]. The extractor will avoid checking this type.
 */
object DBList extends DBCompanion[Seq[Any], com.mongodb.BasicDBList] {

  override def apply(values: Seq[Any]): BasicDBList = {
    val list = new BasicDBList()
    for ((value, index) <- values.zipWithIndex) {
      list.put(index, value)
    }
    list
  }

  override def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: BasicDBObject => false  // Don't match on DBObject
    case _: BasicDBList | _: Traversable[_] | _: Array[_] => true
    case _ => false
  }

  override def unapply(value: Any): Option[BasicDBList] = value match {
    // this must come first to avoid being caught as a Traversable
    case null | _: BasicDBObject => None  // Don't match on DBObject
    case x: BasicDBList =>          Some(x)
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Traversable[_] =>       Some(apply(x.toSeq))
    case x: Array[_] =>             Some(apply(x))
    case _ => None
  }
}

/**
 * Extracts a [[com.mongodb.BasicDBObject]] from a mongodb or raw Scala Map, if possible.
 *
 * @note this will also match a Map of String, but only if this extractor is used
 *       before the [[BasicDBList]] extractor.
 *
 * [[com.mongodb.BasicDBObject]] is the underlying representation for Mongo objects and
 * extends from [[com.mongodb.DBObject]]. The extractor will exploit this inheritance.
 */
object DBObject extends DBCompanion[Map[String, Any], com.mongodb.BasicDBObject] {

  override def apply(fields: Map[String, Any]): com.mongodb.BasicDBObject = {
    val dbo = new BasicDBObject()
    for ((key, value) <- fields) {
      dbo.put(key, value)
    }
    dbo
  }

  override def matches(value: Any): Boolean = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: BasicDBList => false
    case _: com.mongodb.DBObject => true  // BasicDBObject extends from DBObject, so this will match both
    case x: Map[_, _] => x.keys forall (_.isInstanceOf[String])
    case _ => false
  }

  override def unapply(value: Any): Option[com.mongodb.BasicDBObject] = value match {
    // this must come first to avoid being caught as a Traversable or DBObject
    case null | _: BasicDBList =>   None
    case x: com.mongodb.BasicDBObject => Some(x)
    case x: com.mongodb.DBObject => Some(apply(x.toMap.collect { case (k: String, v: Any) => (k, v) }.toMap[String, Any]))
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Map[_, _] if x.keys forall (_.isInstanceOf[String]) =>
      val fields = x map { case (k, v) => (k.asInstanceOf[String], v) }
      Some(apply(fields))
    case _ => None
  }
}

/**
 * Extracts an [[Array]] of [[Byte]] from a raw value, if possible.
 */
object DBBinary extends DBExtractor[Array[Byte]] {

  override def matches(value: Any): Boolean = value match {
    case null => false
    case _: org.bson.types.Binary => true
    case arr: Array[_] =>
      arr.length == 0 || arr(0).isInstanceOf[Byte]
    case _ => false
  }

  override def unapply(value: Any): Option[Array[Byte]] = value match {
    case null => None
    case x: org.bson.types.Binary => Some(x.getData)
    case x: Array[_] if x.length == 0 || x(0).isInstanceOf[Byte] => Some(x.asInstanceOf[Array[Byte]])
    case _ => None
  }
}

/**
 * Extracts a [[java.util.regex.Pattern]] from a raw value, if possible.
 */
object DBRegex extends DBExtractor[Regex] {

  def matches(value: Any): Boolean = value match {
    case null => false
    case _: java.util.regex.Pattern | _: Regex => true
    case _ => false
  }

  def unapply(value: Any): Option[Regex] = value match {
    case null => None
    case x: java.util.regex.Pattern => Some(x.pattern.r)
    // unlikely to come out of something loaded from the database, but possible from a manually created DBObject
    case x: Regex => Some(x)
    case _ => None
  }
}

/**
 * Extracts a [[org.joda.time.DateTime]] from a raw value, if possible.
 */
object DBDate extends DBExtractor[DateTime] {

  def matches(value: Any): Boolean = value match {
    case null => false
    case _: java.util.Date | _: DateTime => true
    case _ => false
  }

  def unapply(value: Any): Option[DateTime] = value match {
    case null => None
    case x: java.util.Date => Some(new DateTime(x.getTime, DateTimeZone.UTC))
    case x: DateTime => Some(x)
    case _ => None
  }
}

/**
 * Extend this to add to scope the MongoDB-friendly value extractors (see [[DBExtractor]]).
 */
trait MongoDBExtractors extends DBValueExtractors with DefaultDBValueExtractor {

  override type DBListType = BasicDBList
  override type DBObjectType = BasicDBObject

  override protected def isEmpty(dbo: BasicDBObject): Boolean = dbo.isEmpty

  override val DBList: DBExtractor[BasicDBList] = adt.bson.mongodb.DBList
  override val DBObject: DBExtractor[BasicDBObject] = adt.bson.mongodb.DBObject
  override val DBBinary: DBExtractor[Array[Byte]] = adt.bson.mongodb.DBBinary
  override val DBRegex: DBExtractor[Regex] = adt.bson.mongodb.DBRegex
  override val DBDate: DBExtractor[DateTime] = adt.bson.mongodb.DBDate
}

object MongoDBExtractors extends MongoDBExtractors