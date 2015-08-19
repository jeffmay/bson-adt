package adt.bson

import org.bson.types.ObjectId
import org.joda.time.{DateTimeZone, DateTime}

import scala.annotation.implicitNotFound
import scala.collection.generic
import scala.language.implicitConversions
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex

/**
 * Bson deserializer: write an implicit to define a BsonValue deserializer for any Scala type.
 */
@implicitNotFound("No Bson deserializer found for type ${A}. " +
  "Implement or import an implicit BsonReads or BsonFormat for this type."
)
trait BsonReads[A] {

  /**
   * Convert the [[BsonValue]] into an object of type [[A]]
   */
  def reads(bson: BsonValue): A

  def map[B](f: A => B): BsonReads[B] = BsonReads(reads _ andThen f)

  def flatMap[B](f: A => BsonReads[B]): BsonReads[B] = BsonReads[B] {
    bson =>
      val a = this reads bson
      f(a) reads bson
  }

  def filter(error: Throwable)(f: A => Boolean): BsonReads[A] = {
    BsonReads[A] { bson =>
      val a = this reads bson
      if (f(a)) a
      else throw error
    }
  }

  def filterNot(error: Throwable)(f: A => Boolean): BsonReads[A] = {
    filter(error)(!f(_))
  }

  def collect[B](error: Throwable)(f: PartialFunction[A, B]) = {
    BsonReads[B] { bson =>
      val a = this reads bson
      if (f isDefinedAt a) f(a)
      else throw error
    }
  }

  def orElse(aReader: BsonReads[A]): BsonReads[A] = {
    BsonReads[A] { bson =>
      val result = Try(this reads bson) orElse Try(aReader reads bson)
      result.get
    }
  }

  def compose[B <: BsonValue](bReader: BsonReads[B]): BsonReads[A] = {
    BsonReads[A] { bson =>
      val b = bReader reads bson
      this reads b
    }
  }

  def andThen[B](bReader: BsonReads[B])(implicit witness: A <:< BsonValue): BsonReads[B] = {
    BsonReads[B] { bson =>
      val a = witness(this reads bson)
      bReader reads a
    }
  }
}

object BsonReads extends DefaultBsonReads {

  def apply[A](f: BsonValue => A): BsonReads[A] = new BsonReads[A] {
    override def reads(bson: BsonValue): A = f(bson)
  }

  def of[A: BsonReads]: BsonReads[A] = implicitly
}

/**
 * Default Bson Deserializers
 */
trait DefaultBsonReads {

  // TODO: Tests for loss of precision exceptions

  private def validShort(n: Long): Short = {
    if (n <= Short.MaxValue && n >= Short.MinValue) n.toShort
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to a Short")
  }

  private def validShort(n: Double): Short = {
    if (n.isValidShort) n.toShort
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to a Short")
  }

  private def validInt(n: Long): Int = {
    if (n <= Int.MaxValue && n >= Int.MinValue) n.toInt
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to an Int")
  }

  private def validInt(n: Double): Int = {
    if (n.isValidInt) n.toInt
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to an Int")
  }

  private def validLong(n: Double): Long = {
    if (n.isWhole) n.toLong
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to an Long")
  }

  private def validFloat(n: Long): Float = {
    if (n > Float.NegativeInfinity && n < Float.PositiveInfinity) n.toFloat
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to an Float")
  }

  private def validFloat(n: Double): Float = {
    if (n > Float.NegativeInfinity && n < Float.PositiveInfinity) n.toFloat
    else throw new NumberFormatException(s"Loss of precision detected when converting $n to an Float")
  }

  implicit object BsonReadsBoolean extends BsonReads[Boolean] {
    override def reads(bson: BsonValue): Boolean = bson match {
      case BsonBoolean(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.boolean", bson)
    }
  }

  implicit object BsonReadsString extends BsonReads[String] {
    override def reads(bson: BsonValue): String = bson match {
      case BsonString(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.string", bson)
    }
  }

  implicit object BsonReadsBinary extends BsonReads[Array[Byte]] {
    override def reads(bson: BsonValue): Array[Byte] = bson match {
      case BsonBinary(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.binary", bson)
    }
  }

  implicit object BsonReadsRegex extends BsonReads[Regex] {
    override def reads(bson: BsonValue): Regex = bson match {
      case BsonRegex(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.regex", bson)
    }
  }

  implicit object BsonReadsObjectId extends BsonReads[ObjectId] {
    override def reads(bson: BsonValue): ObjectId = bson match {
      case BsonObjectId(value) => value
      case BsonString(value) => new ObjectId(value)
      case _ => throw new UnexpectedBsonException("error.expected.oid", bson)
    }
  }

  // Note: Intentionally leaving out BsonReadsDate, because java.util.Date sucks like a hurricane
  //       and we should favor using org.joda.time.DateTime in the code.

  implicit object BsonReadsDateTime extends BsonReads[DateTime] {
    override def reads(bson: BsonValue): DateTime = bson match {
      // Mongo always stores DateTime in UTC, but let's just make it clear here in code
      case BsonDate(value) => value withZone DateTimeZone.UTC
      case _ => throw new UnexpectedBsonException("error.expected.datetime", bson)
    }
  }

  implicit object BsonReadsShort extends BsonReads[Short] {
    override def reads(bson: BsonValue): Short = bson match {
      case BsonInt(value) => validShort(value)
      case BsonLong(value) => validShort(value)
      case BsonDouble(value) => validShort(value)
      case _ => throw new UnexpectedBsonException("error.expected.int", bson)  // BSON does not have a type Short
    }
  }

  implicit object BsonReadsInt extends BsonReads[Int] {
    override def reads(bson: BsonValue): Int = bson match {
      case BsonInt(value) => value
      case BsonLong(value) => validInt(value)
      case BsonDouble(value) => validInt(value)
      case _ => throw new UnexpectedBsonException("error.expected.int", bson)
    }
  }

  implicit object BsonReadsLong extends BsonReads[Long] {
    override def reads(bson: BsonValue): Long = bson match {
      case BsonInt(value) => value.toLong
      case BsonLong(value) => value
      case BsonDouble(value) => validLong(value)
      case _ => throw new UnexpectedBsonException("error.expected.long", bson)
    }
  }

  implicit object BsonReadsFloat extends BsonReads[Float] {
    override def reads(bson: BsonValue): Float = bson match {
      case BsonInt(value) => validFloat(value)
      case BsonLong(value) => validFloat(value)
      case BsonDouble(value) => validFloat(value)
      case _ => throw new UnexpectedBsonException("error.expected.number", bson)  // BSON does not have a type Float
    }
  }

  implicit object BsonReadsDouble extends BsonReads[Double] {
    override def reads(bson: BsonValue): Double = bson match {
      case BsonInt(value) => value.toDouble
      case BsonLong(value) => value.toDouble
      case BsonDouble(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.number", bson)
    }
  }

  implicit object BsonReadsBigDecimal extends BsonReads[BigDecimal] {
    override def reads(bson: BsonValue): BigDecimal = bson match {
      case BsonInt(value) => value
      case BsonLong(value) => value
      case BsonDouble(value) => value
      case _ => throw new UnexpectedBsonException("error.expected.number", bson)
    }
  }

  implicit object BsonReadsBsonValue extends BsonReads[BsonValue] {
    override def reads(bson: BsonValue): BsonValue = bson
  }

  implicit object BsonReadsBsonBoolean extends BsonReads[BsonBoolean] {
    override def reads(bson: BsonValue): BsonBoolean = bson match {
      case bool: BsonBoolean => bool
      case _ => throw new UnexpectedBsonException("error.expected.boolean", bson)
    }
  }

  implicit object BsonReadsBsonInt extends BsonReads[BsonInt] {
    override def reads(bson: BsonValue): BsonInt = bson match {
      case int: BsonInt => int
      case _ => throw new UnexpectedBsonException("error.expected.int", bson)
    }
  }

  implicit object BsonReadsBsonLong extends BsonReads[BsonLong] {
    override def reads(bson: BsonValue): BsonLong = bson match {
      case long: BsonLong => long
      case _ => throw new UnexpectedBsonException("error.expected.long", bson)
    }
  }

  implicit object BsonReadsBsonNumber extends BsonReads[BsonNumber] {
    override def reads(bson: BsonValue): BsonNumber = bson match {
      case num: BsonNumber => num
      case _ => throw new UnexpectedBsonException("error.expected.number", bson)
    }
  }

  implicit object BsonReadsBsonString extends BsonReads[BsonString] {
    override def reads(bson: BsonValue): BsonString = bson match {
      case str: BsonString => str
      case _ => throw new UnexpectedBsonException("error.expected.string", bson)
    }
  }

  implicit object BsonReadsBsonDate extends BsonReads[BsonDate] {
    override def reads(bson: BsonValue): BsonDate = bson match {
      case date: BsonDate => date
      case _ => throw new UnexpectedBsonException("error.expected.date", bson)
    }
  }

  implicit object BsonReadsBsonObjectId extends BsonReads[BsonObjectId] {
    override def reads(bson: BsonValue): BsonObjectId = bson match {
      case oid: BsonObjectId => oid
      case _ => throw new UnexpectedBsonException("error.expected.objectid", bson)
    }
  }

  implicit object BsonReadsBsonRegex extends BsonReads[BsonRegex] {
    override def reads(bson: BsonValue): BsonRegex = bson match {
      case regex: BsonRegex => regex
      case _ => throw new UnexpectedBsonException("error.expected.objectid", bson)
    }
  }

  implicit object BsonReadsBsonArray extends BsonReads[BsonArray] {
    override def reads(bson: BsonValue): BsonArray = bson match {
      case arr: BsonArray => arr
      case _ => throw new UnexpectedBsonException("error.expected.object", bson)
    }
  }

  implicit object BsonReadsBsonObject extends BsonReads[BsonObject] {
    override def reads(bson: BsonValue): BsonObject = bson match {
      case obj: BsonObject => obj
      case _ => throw new UnexpectedBsonException("error.expected.object", bson)
    }
  }

  implicit object BsonReadsBsonContainer extends BsonReads[BsonContainer] {
    override def reads(bson: BsonValue): BsonContainer = bson match {
      case cons: BsonContainer => cons
      case _ => throw new UnexpectedBsonException("error.expected.object", bson)
    }
  }

  implicit object BsonReadsBsonPrimitive extends BsonReads[BsonPrimitive] {
    override def reads(bson: BsonValue): BsonPrimitive = bson match {
      case prim: BsonPrimitive => prim
      case _ => throw new UnexpectedBsonException("error.expected.object", bson)
    }
  }

  /**
   * Generic exception wrapping deserializer.
   */
  implicit def tryReads[A](implicit aReader: BsonReads[A]): BsonReads[Try[A]] = new BsonReads[Try[A]] {
    override def reads(bson: BsonValue): Try[A] = Try(aReader reads bson)
  }

  /**
   * Generic deserializer for collections types.
   */
  implicit def traversableReads[C[_], A](
    implicit
      bf: generic.CanBuildFrom[C[_], A, C[A]],
      aReader: BsonReads[A]
    ): BsonReads[C[A]] = {
    new BsonReads[C[A]] {
      override def reads(bson: BsonValue): C[A] = bson match {
        case BsonArray(values) =>
          val builder = bf()
          val valIter = values.iterator
          while (valIter.hasNext) {
            val bson = valIter.next()
            val a = aReader reads bson
            builder += a
          }
          builder.result()
        case _ => throw new UnexpectedBsonException("error.expected.array", bson)
      }
    }
  }

  /**
   * Deserializer for Array[T] types.
   */
  implicit def arrayReads[T: BsonReads: ClassTag]: BsonReads[Array[T]] = new BsonReads[Array[T]] {
    def reads(json: BsonValue): Array[T] = json.as[List[T]].toArray
  }

  /**
   * Generic deserializer for String-keyed maps.
   */
  implicit def mapReads[V](implicit vReader: BsonReads[V]): BsonReads[Map[String, V]] = {
    new BsonReads[Map[String, V]] {
      override def reads(bson: BsonValue): Map[String, V] = bson match {
        case BsonObject(values) => values mapValues vReader.reads
        case _ => throw new UnexpectedBsonException("error.expected.object", bson)
      }
    }
  }
}