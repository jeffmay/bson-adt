package adt.bson.scalacheck

import adt.bson._
import adt.bson.scalacheck.RegexGenerators._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.scalacheck.Arbitrary._
import org.scalacheck.Shrink._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.language.implicitConversions
import scala.util.matching.Regex

object BsonValueGenerators extends BsonValueGenerators
trait BsonValueGenerators extends CommonGenerators {

  protected def maxSize: Int = 3
  protected def maxDepth: Int = 3

  implicit val arbBsonValue: Arbitrary[BsonValue] = Arbitrary {
    for {
      depth <- Gen.choose(0, maxSize)
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

  implicit def arbBsonNumber: Arbitrary[BsonNumber] = arbBsonDouble.asInstanceOf[Arbitrary[BsonNumber]]

  implicit val arbBsonDouble: Arbitrary[BsonDouble] = Arbitrary {
    arbitrary[Double] map BsonDouble
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
      depth <- Gen.choose(0, maxDepth)
      array <- genBsonArray(depth)
    } yield array
  }

  implicit val arbBsonObject: Arbitrary[BsonObject] = Arbitrary {
    for {
      depth <- Gen.choose(0, maxDepth)
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

  implicit def shrinkBsonNumber: Shrink[BsonNumber] = shrinkBsonDouble.asInstanceOf[Shrink[BsonNumber]]

  implicit val shrinkBsonDouble: Shrink[BsonDouble] = Shrink { bson =>
    Stream(BsonDouble(0))  // just try 0 and give up
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
    shrinkContainer2[Map, String, BsonValue].shrink(bson.value).map { fields =>
      BsonObject(fields)
    }
  }

  implicit val shrinkBsonValue: Shrink[BsonValue] = Shrink {
    case it: BsonObject => shrink(it)
    case it: BsonArray => shrink(it)
    case it: BsonBoolean => shrink(it)
    case it: BsonInt => shrink(it)
    case it: BsonLong => shrink(it)
    case it: BsonDouble => shrink(it)
    case it: BsonString => shrink(it)
    case it: BsonObjectId => shrink(it)
    case it: BsonBinary => shrink(it)
    case it: BsonDate => shrink(it)
    case it: BsonRegex => shrink(it)
    case BsonNull => Stream.empty
    case BsonUndefined() => Stream.empty
  }

  def genElementMax: Gen[Int] = Gen.choose(0, maxSize)

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
    genArraySize: Gen[Int] = genElementMax): Gen[BsonArray] = {
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
    genObjectSize: Gen[Int] = genElementMax): Gen[BsonObject] = {
    require(depth >= 0, "depth cannot be negative")
    val genFieldValue =
      if (depth == 0) genBsonPrimitive()
      else genBsonObject(depth - 1, genObjectSize)
    for {
      n <- genObjectSize
      fields <- Gen.listOfN(n, Gen.zip(Gen.identifier, genFieldValue))
    } yield BsonObject(fields.toMap)
  }

  def genBsonValue(
    depth: Int = 5,
    genObjectSize: Gen[Int] = genElementMax,
    genArraySize: Gen[Int] = genElementMax): Gen[BsonValue] = {
    require(depth >= 0, "depth cannot be negative")
    if (depth == 0) genBsonPrimitive()
    else Gen.oneOf(
      genBsonPrimitive(),
      genBsonArray(depth, genObjectSize),
      genBsonObject(depth, genObjectSize)
    )
  }
}
