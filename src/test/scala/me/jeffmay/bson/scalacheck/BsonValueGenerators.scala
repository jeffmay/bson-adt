package me.jeffmay.bson.scalacheck

import me.jeffmay.bson._
import org.bson.types.{Binary, ObjectId}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalacheck.{Arbitrary, Gen}

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

  implicit def arbBinary(implicit arbBytes: Arbitrary[Array[Byte]]): Arbitrary[Binary] = Arbitrary {
    arbBytes.arbitrary.map(new Binary(_))
  }

  implicit def arbObjectId(implicit arbDate: Arbitrary[DateTime]): Arbitrary[ObjectId] = Arbitrary {
    for {
      when <- arbDate.arbitrary
    } yield new ObjectId(new java.util.Date(when.getMillis))
  }

  implicit val arbBsonValue: Arbitrary[BsonValue] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      value <- genBsonValue(depth)
    } yield value
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

  def genBson[A: BsonWrites](genValue: Gen[A]): Gen[BsonValue] = for {
    value <- genValue
  } yield Bson.toBson(value)

  @inline def genBson[A: Arbitrary: BsonWrites]: Gen[BsonValue] = genBson(Arbitrary.arbitrary[A])

  def genBsonPrimitive(): Gen[BsonValue] = Gen.oneOf(
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
    genArraySize: Gen[Int] = Gen.choose(0, 10)): Gen[BsonArray] = {
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
    genObjectSize: Gen[Int] = Gen.choose(0, 10)): Gen[BsonObject] = {
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
