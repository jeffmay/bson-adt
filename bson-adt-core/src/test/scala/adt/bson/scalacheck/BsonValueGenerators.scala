package adt.bson.scalacheck

import adt.bson._
import org.bson.types.{Binary, ObjectId}
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.scalacheck.Arbitrary._
import org.scalacheck.Shrink._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.language.implicitConversions
import scala.util.matching.Regex

trait BsonValueGenerators extends RegexGenerators {

  implicit def arbitraryDateTimeZone: Arbitrary[DateTimeZone] = Arbitrary {
    for {
      id <- Gen.oneOf(DateTimeZone.getAvailableIDs.toArray[String](Array.empty).toSeq)
    } yield DateTimeZone.forID(id)
  }

  implicit def arbitraryDateTime(implicit genDateTimeZone: Arbitrary[DateTimeZone]): Arbitrary[DateTime] = {
    Arbitrary(
      for {
        millis <- Gen.choose(0L, Long.MaxValue)
        dateTimeZone <- genDateTimeZone.arbitrary
      } yield new DateTime(millis, dateTimeZone)
    )
  }

  implicit def arbitraryLocalDateTime: Arbitrary[LocalDateTime] = {
    Arbitrary(
      for {
        millis <- Gen.choose(0L, Long.MaxValue)
      } yield new LocalDateTime(millis)
    )
  }

  private def halves[T](n: T)(implicit integral: Integral[T]): Stream[T] = {
    import integral._
    if (n == zero) Stream.empty
    else n #:: halves(n / fromInt(2))
  }

  private def shrinkSameSign[T](value: T)(implicit integral: Integral[T]): Stream[T] = {
    import integral._
    zero #:: halves(value).map(value - _)
  }

  implicit val shrinkDateTime: Shrink[DateTime] = Shrink { datetime =>
    val shrinkMillis = shrinkSameSign(datetime.getMillis)
    shrinkMillis map { new DateTime(_, datetime.getZone) }
  }

  implicit def arbBinary(implicit arbBytes: Arbitrary[Array[Byte]]): Arbitrary[Binary] = Arbitrary {
    arbBytes.arbitrary.map(new Binary(_))
  }

  implicit def arbObjectId(implicit arbDate: Arbitrary[java.util.Date]): Arbitrary[ObjectId] = Arbitrary {
    for {
      date <- arbDate.arbitrary
    } yield new ObjectId(date)
  }

  // The only way in which the ObjectId could cause a property to fail is if someone is using
  // the timestamp from the ObjectId in a test.
  implicit val shrinkObjectId: Shrink[ObjectId] = Shrink { oid =>
    val shrinkSeconds = shrinkSameSign(oid.getTimestamp)
    shrinkSeconds map { millis => new ObjectId(new java.util.Date(millis * 1000)) }
  }

  implicit val arbBsonValue: Arbitrary[BsonValue] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      value <- genBsonValue(depth)
    } yield value
  }

  implicit val arbBsonPrimitive: Arbitrary[BsonPrimitive] = Arbitrary {
    genBsonPrimitive()
  }

  implicit val arbBsonBoolean: Arbitrary[BsonBoolean] = Arbitrary {
    Gen.oneOf(true, false) map BsonBoolean
  }

  implicit val arbBsonInt: Arbitrary[BsonInt] = Arbitrary {
    arbitrary[Int] map BsonInt
  }

  implicit val arbBsonLong: Arbitrary[BsonLong] = Arbitrary {
    arbitrary[Long] map BsonLong
  }

  implicit val arbBsonNumber: Arbitrary[BsonNumber] = Arbitrary {
    arbitrary[Double] map BsonNumber
  }

  implicit val arbBsonString: Arbitrary[BsonString] = Arbitrary {
    arbitrary[String] map BsonString
  }

  implicit val arbBsonBinary: Arbitrary[BsonBinary] = Arbitrary {
    arbitrary[Array[Byte]] map BsonBinary
  }

  implicit val arbBsonRegex: Arbitrary[BsonRegex] = Arbitrary {
    arbitrary[Regex] map BsonRegex
  }

  implicit val arbBsonObjectId: Arbitrary[BsonObjectId] = Arbitrary {
    arbitrary[ObjectId] map BsonObjectId
  }

  implicit def arbBsonDate(implicit arbDateTime: Arbitrary[DateTime]): Arbitrary[BsonDate] = Arbitrary {
    arbDateTime.arbitrary map BsonDate
  }

  implicit val arbBsonArray: Arbitrary[BsonArray] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      array <- genBsonArray(depth)
    } yield array
  }

  implicit val arbBsonObject: Arbitrary[BsonObject] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      obj <- genBsonObject(depth)
    } yield obj
  }

  implicit val shrinkBsonBoolean: Shrink[BsonBoolean] = Shrink { bson =>
    shrink(bson.value) map BsonBoolean
  }

  implicit val shrinkBsonInt: Shrink[BsonInt] = Shrink { bson =>
    shrink(bson.value) map BsonInt
  }

  implicit val shrinkBsonLong: Shrink[BsonLong] = Shrink { bson =>
    shrinkSameSign(bson.value) map BsonLong
  }

  implicit val shrinkBsonNumber: Shrink[BsonNumber] = Shrink { bson =>
    Stream(BsonNumber(0))  // just try 0 and give up
  }

  implicit val shrinkBsonString: Shrink[BsonString] = Shrink { bson =>
    shrink(bson.value) map BsonString
  }

  implicit val shrinkBsonBinary: Shrink[BsonBinary] = Shrink { bson =>
    shrink(bson.value) map BsonBinary
  }

  implicit val shrinkBsonDate: Shrink[BsonDate] = Shrink { bson =>
    shrink(bson.value) map BsonDate
  }

  implicit val shrinkBsonObjectId: Shrink[BsonObjectId] = Shrink { bson =>
    shrink(bson.value) map BsonObjectId
  }

  implicit val shrinkBsonArray: Shrink[BsonArray] = Shrink { bson =>
    shrink[Seq[BsonValue]](bson.value) map { values =>
      BsonArray(values)
    }
  }

  implicit val shrinkBsonObject: Shrink[BsonObject] = Shrink { bson =>
    shrink[Map[String, BsonValue]](bson.value) map { fields =>
      BsonObject(fields)
    }
  }

  implicit val shrinkBsonValue: Shrink[BsonValue] = Shrink {
    case it: BsonObject => shrink(it)
    case it: BsonArray => shrink(it)
    case it: BsonBoolean => shrink(it)
    case it: BsonInt => shrink(it)
    case it: BsonLong => shrink(it)
    case it: BsonNumber => shrink(it)
    case it: BsonString => shrink(it)
    case it: BsonObjectId => shrink(it)
    case it: BsonBinary => shrink(it)
    case it: BsonDate => shrink(it)
    case it: BsonRegex => shrink(it)
    case BsonNull => Stream.empty
    case BsonUndefined() => Stream.empty
  }

  private def genBson[A: BsonWrites](genValue: Gen[A]): Gen[BsonPrimitive] = for {
    value <- genValue
  } yield Bson.toBson(value).asInstanceOf[BsonPrimitive]

  @inline private def genBson[A: Arbitrary: BsonWrites]: Gen[BsonPrimitive] = genBson(Arbitrary.arbitrary[A])

  def genBsonPrimitive(): Gen[BsonPrimitive] = Gen.oneOf(
    Gen.const(BsonNull),
    genBson[Boolean],
    genBson[Short],
    genBson[Int],
    genBson[Long],
    genBson[Float],
    genBson[Double],
    genBson[String],
    genBson[Array[Byte]],
    genBson[Regex],
    genBson[ObjectId],
    genBson[DateTime]
  )
  
  def genBsonArray(
    depth: Int = 0,
    genArraySize: Gen[Int] = Gen.choose(0, 5)): Gen[BsonArray] = {
    require(depth >= 0, "depth cannot be negative")
    val genItemValue =
      if (depth == 0) genBsonPrimitive()
      else genBsonArray(depth - 1, genArraySize)
    for {
      n <- genArraySize
      fields <- Gen.listOfN(n, genItemValue)
    } yield BsonArray(fields)
  }

  def genBsonObject(
    depth: Int = 0,
    genObjectSize: Gen[Int] = Gen.choose(0, 5)): Gen[BsonObject] = {
    require(depth >= 0, "depth cannot be negative")
    val genFieldValue =
      if (depth == 0) genBsonPrimitive()
      else genBsonObject(depth - 1, genObjectSize)
    for {
      n <- genObjectSize
      fields <- Gen.listOfN(n, Gen.zip(Gen.alphaStr.suchThat(!_.isEmpty), genFieldValue))
    } yield BsonObject(fields.toMap)
  }

  def genBsonValue(
    depth: Int = 5,
    genObjectSize: Gen[Int] = Gen.choose(0, 5),
    genArraySize: Gen[Int] = Gen.choose(0, 5)): Gen[BsonValue] = {
    require(depth >= 0, "depth cannot be negative")
    if (depth == 0) genBsonPrimitive()
    else Gen.oneOf(
      genBsonPrimitive(),
      genBsonArray(depth, genObjectSize),
      genBsonObject(depth, genObjectSize)
    )
  }
}
