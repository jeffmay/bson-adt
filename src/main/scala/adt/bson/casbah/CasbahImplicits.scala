package adt.bson.casbah

import com.mongodb.casbah.Imports._
import adt.bson._

import scala.language.{higherKinds, implicitConversions}

/**
 * Enables implicit syntax for converting [[BsonValue]] to and from Casbah / Mongo Java typed values.
 */
trait CasbahImplicits {

  import adt.bson.casbah.CasbahImplicits._

  implicit def toBsonObjectOps(bson: BsonObject): BsonObjectOps = new BsonObjectOps(bson)

  implicit def toBsonArrayOps(bson: BsonArray): BsonArrayOps = new BsonArrayOps(bson)

  implicit def toBsonValueOps(bson: BsonValue): BsonValueOps = new BsonValueOps(bson)

  implicit def toBsonWriteableOps[T: BsonWrites](writeable: T): BsonWriteableOps[T] = new BsonWriteableOps(writeable)

  implicit def toDBListBsonOps(dbList: MongoDBList): DBListBsonOps = new DBListBsonOps(dbList)

  implicit def toDBObjectBsonOps(dbo: MongoDBObject): DBObjectBsonOps = new DBObjectBsonOps(dbo)

  implicit def toDBObjectBsonOps(dbo: DBObject): DBObjectBsonOps = new DBObjectBsonOps(dbo)

  def bsonValue(value: Any): BsonValue = value match {
    case null => BsonNull
    case x: String => BsonString(x)
    case x: ObjectId => BsonObjectId(x)
    case x: java.lang.Boolean => BsonBoolean(x)
    case x: java.lang.Integer => BsonInt(x)
    case x: java.lang.Long => BsonLong(x)
    case x: java.lang.Double => BsonNumber(x)
    case x: java.lang.Float => BsonNumber(x.toDouble)
    case DBBinary(bin) => BsonBinary(bin)
    case DBRegex(re) => BsonRegex(re)
    case DBDate(date) => BsonDate(date)
    case DBObject(dbo) => dbo.toBsonObject
    case DBList(arr) => arr.toBsonArray
    case _ => throw new MatchError(
      s"No conversion to BsonValue for instance of ${value.getClass.getName}: $value. " +
      "This is programmer error. Please update CasbahImplicits.bsonValue to support this type"
    )
  }

  /**
   * Convert a Bson Value to a value that is safe for Casbah's Mongo driver.
   *
   * Exists for consistency with dbObject method.
   *
   * @return the underlying value as Mongo would store it.
   */
  def dbValue(bson: BsonValue): Any = {
    val value = bson match {
      case subDoc: BsonObject => subDoc.toDBObject
      case subArr: BsonArray => subArr.toMongoDBList
      case BsonRegex(regex) => regex.pattern
      case BsonDate(datetime) => datetime.toDate
      case _ => bson.value
    }
    assert(
      DBValue matches value,
      s"Invalid DBValue conversion result for $bson" +
      "This is programmer error. Please update CasbahImplicits.dbValue to handle this type"
    )
    value
  }

  /**
   * Convert a value to a DBObject instead of a BsonObject using a Bson writer.
   *
   * Useful when you can't trust the implicit conversion to apply.
   */
  def dbObject[A](value: A, pruned: Boolean = false)(implicit writer: BsonObjectWrites[A]): DBObject = {
    var bson = writer writes value
    if (pruned) {
      bson = bson.pruned
    }
    bson.toDBObject
  }

}

object CasbahImplicits extends CasbahImplicits {
  
  class BsonObjectOps(val bson: BsonObject) extends AnyVal {

    def toDBObject: DBObject = {
      val fields: List[(String, Any)] = bson.value.mapValues(dbValue).toList
      MongoDBObject(fields)
    }

    def toMongoDBObject: MongoDBObject = toDBObject

  }

  class BsonArrayOps(val bson: BsonArray) extends AnyVal {

    def toMongoDBList: MongoDBList = {
      MongoDBList(bson.value map dbValue: _*)
    }
  }

  class BsonValueOps(val bson: BsonValue) extends AnyVal {

    @inline final def toDBValue: Any = dbValue(bson)
  }

  /**
   * Enables the syntax for building [[MongoDBObject]].
   *
   * For example:
   * {{{
   *   val a = MongoDBObject(
   *     "field" ->: value
   *   )
   *   val b = MongoDBObject(
   *     "field" -> dbValue(Bson.toBson(value))
   *   )
   *   a == b
   * }}}
   *
   * @param writeable the object that can be written to a BsonValue
   * @param writer the Bson serializer
   */
  class BsonWriteableOps[T](writeable: T)(implicit writer: BsonWrites[T]) {

    /**
     * Produce a tuple of the field name to a [[DBValue]] using an intermediate [[BsonValue]].
     *
     * @param key the field name of the tuple
     */
    def ->:(key: String): (String, Any) = {
      val bson = writer writes writeable
      (key, dbValue(bson))
    }
  }
  
  class DBListBsonOps(val dbList: MongoDBList) extends AnyVal {

    /**
     * Converts a MongoDBList into a [[BsonArray]]
     */
    def toBsonArray: BsonArray = {
      // Casbah is actin' stupid here, so we'll convert the underlying list
      val underlyingSeq = dbList.underlying.toStream
      val values = underlyingSeq map bsonValue
      BsonArray(values)
    }

    /**
     * Provided an implicit deserializer of the right type, reads this MongoDBList as an instance of A.
     */
    @inline final def readAs[A: BsonReads]: A = toBsonArray.as[A]
  }

  class DBObjectBsonOps(val dbo: MongoDBObject) extends AnyVal {

    /**
     * Converts a MongoDBObject into a [[BsonObject]]
     */
    def toBsonObject: BsonObject = {
      val fields = dbo.mapValues(bsonValue).toMap
      BsonObject(fields)
    }

    /**
     * Provided an implicit deserializer of the right type, reads this MongoDBObject as an instance of A.
     */
    @inline final def readAs[A: BsonReads]: A = toBsonObject.as[A]

    /**
     * A convenience to implicitly convert a DBObject to BsonObject in order to inspect a field.
     *
     * @param key the name of the field to inspect, or BsonUndefined if the field doesn't exist.
     */
    @inline final def \(key: String): BsonValue = toBsonObject \ key

    /**
     * Read a field of this DBObject using a Bson reader.
     *
     * This can be helpful since DBObject has a .as[A] method that hides an implicit
     * conversion to BsonValue, which has a .as[A] method
     */
    def readFieldAs[A](fieldName: String)(implicit reader: BsonReads[A]): A = {
      reader reads bsonValue(DBValue(dbo get fieldName))
    }
  }
}