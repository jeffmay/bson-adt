package adt.bson

import org.bson.BsonElement

import scala.collection.JavaConversions.{collectionAsScalaIterable, seqAsJavaList}

/**
 * Provides handy type aliases for consistency and avoiding namespace clashes.
 *
 * Just `import adt.bson.mongo._` to have access to these types, along with
 * handy object builders with the same names.
 */
object JavaBsonValues extends JavaBsonValues
trait JavaBsonValues {
  type JavaBsonArray     = org.bson.BsonArray
  type JavaBsonBinary    = org.bson.BsonBinary
  type JavaBsonBoolean   = org.bson.BsonBoolean
  type JavaBsonDate      = org.bson.BsonDateTime
  type JavaBsonDocument  = org.bson.BsonDocument
  type JavaBsonField     = org.bson.BsonElement
  type JavaBsonObject    = org.bson.BsonDocument
  type JavaBsonObjectId  = org.bson.BsonObjectId
  type JavaBsonInt       = org.bson.BsonInt32
  type JavaBsonLong      = org.bson.BsonInt64
  type JavaBsonNumber    = org.bson.BsonDouble
  type JavaBsonRegex     = org.bson.BsonRegularExpression
  type JavaBsonString    = org.bson.BsonString
  type JavaBsonValue     = org.bson.BsonValue
  type JavaBsonUndefined = org.bson.BsonUndefined
  type JavaBsonNull      = org.bson.BsonNull
  val  JavaBsonNull      = org.bson.BsonNull.VALUE
}

/**
 * Builds and extracts [[org.bson.BsonArray]] values in a more idiomatic Scala fashion.
 */
object JavaBsonArray extends (Seq[JavaBsonValue] => JavaBsonArray) {
  override def apply(elements: Seq[JavaBsonValue]): JavaBsonArray = new JavaBsonArray(elements)
  def unapply(bson: Any): Option[Seq[JavaBsonValue]] = bson match {
    case arr: JavaBsonArray => Some(arr.getValues.toStream)
    case _ => None
  }
}

/**
 * Builds and extracts [[org.bson.BsonBinary]] values in a more idiomatic Scala fashion.
 */
object JavaBsonBinary extends (Array[Byte] => JavaBsonBinary) {
  override def apply(arr: Array[Byte]): JavaBsonBinary = new JavaBsonBinary(arr)
  def unapply(bson: Any): Option[Array[Byte]] = bson match {
    case bin: JavaBsonBinary => Some(bin.getData)
    case _ => None
  }
}

/**
 * Builds and extracts [[org.bson.BsonDocument]] values in a more idiomatic Scala fashion.
 */
object JavaBsonDocument extends (Iterable[(String, JavaBsonValue)] => JavaBsonDocument) {
  override def apply(fields: Iterable[(String, JavaBsonValue)]): JavaBsonDocument = {
    val iter = fields.iterator
    val elements = new java.util.LinkedList[BsonElement]()
    while (iter.hasNext) {
      val (k, v) = iter.next()
      elements.add(new BsonElement(k, v))
    }
    new JavaBsonDocument(elements)
  }
  def unapply(bson: Any): Option[Seq[(String, JavaBsonValue)]] = bson match {
    case doc: JavaBsonDocument =>
      val entities = doc.entrySet().toStream.map(e => (e.getKey, e.getValue))
      Some(entities)
    case _ => None
  }
}
