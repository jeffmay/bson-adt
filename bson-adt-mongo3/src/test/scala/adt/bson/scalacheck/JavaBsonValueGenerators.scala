package adt.bson.scalacheck

import adt.bson.mongo._
import adt.bson.scalacheck.RegexGenerators._
import adt.bson.{JavaBsonDocument, Bson, BsonWrites}
import org.bson._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.scalacheck.Arbitrary._
import org.scalacheck.Shrink._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.collection.JavaConversions.{collectionAsScalaIterable, seqAsJavaList, mapAsScalaMap}
import scala.util.matching.Regex

object JavaBsonValueGenerators extends JavaBsonValueGenerators
trait JavaBsonValueGenerators extends CommonGenerators {

  implicit val arbJavaBsonValue: Arbitrary[JavaBsonValue] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      value <- genJavaBsonValue(depth)
    } yield value
  }

  implicit val arbJavaBsonBoolean: Arbitrary[JavaBsonBoolean] = Arbitrary {
    Gen.oneOf(true, false) map (new JavaBsonBoolean(_))
  }

  implicit val arbJavaBsonInt: Arbitrary[JavaBsonInt] = Arbitrary {
    arbitrary[Int] map (new JavaBsonInt(_))
  }

  implicit val arbJavaBsonLong: Arbitrary[JavaBsonLong] = Arbitrary {
    arbitrary[Long] map (new JavaBsonLong(_))
  }

  implicit val arbJavaBsonNumber: Arbitrary[JavaBsonNumber] = Arbitrary {
    arbitrary[Double] map (new JavaBsonNumber(_))
  }

  implicit val arbJavaBsonString: Arbitrary[JavaBsonString] = Arbitrary {
    arbitrary[String] map (new JavaBsonString(_))
  }

  implicit val arbJavaBsonBinary: Arbitrary[JavaBsonBinary] = Arbitrary {
    arbitrary[Array[Byte]] map (new JavaBsonBinary(_))
  }

  implicit val arbJavaBsonRegex: Arbitrary[JavaBsonRegex] = Arbitrary {
    // TODO: Keep flags
    arbitrary[Regex] map (re => new JavaBsonRegex(re.pattern.pattern()))
  }

  implicit val arbJavaBsonObjectId: Arbitrary[JavaBsonObjectId] = Arbitrary {
    arbitrary[ObjectId] map (new JavaBsonObjectId(_))
  }

  implicit def arbJavaBsonDate(implicit arbDateTime: Arbitrary[DateTime]): Arbitrary[JavaBsonDate] = Arbitrary {
    Gen.posNum[Long] map (new JavaBsonDate(_))
  }

  implicit val arbJavaBsonArray: Arbitrary[JavaBsonArray] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      array <- genJavaBsonArray(depth)
    } yield array
  }

  implicit val arbJavaBsonDocument: Arbitrary[JavaBsonDocument] = Arbitrary {
    for {
      depth <- Gen.choose(0, 3)
      obj <- genJavaBsonDocument(depth)
    } yield obj
  }

  implicit val shrinkJavaBsonBoolean: Shrink[JavaBsonBoolean] = Shrink { bson =>
    shrink(bson.getValue) map (new JavaBsonBoolean(_))
  }

  implicit val shrinkJavaBsonInt: Shrink[JavaBsonInt] = Shrink { bson =>
    shrink(bson.intValue()) map (new JavaBsonInt(_))
  }

  implicit val shrinkJavaBsonLong: Shrink[JavaBsonLong] = Shrink { bson =>
    shrinkSameSign(bson.longValue()) map (new JavaBsonLong(_))
  }

  implicit val shrinkJavaBsonNumber: Shrink[JavaBsonNumber] = Shrink { bson =>
    Stream(new JavaBsonNumber(0))  // just try 0 and give up
  }

  implicit val shrinkJavaBsonString: Shrink[JavaBsonString] = Shrink { bson =>
    shrink(bson.getValue) map (new JavaBsonString(_))
  }

  implicit val shrinkJavaBsonBinary: Shrink[JavaBsonBinary] = Shrink { bson =>
    shrink(bson.getData) map (new JavaBsonBinary(_))
  }

  implicit val shrinkJavaBsonDate: Shrink[JavaBsonDate] = Shrink { bson =>
    shrink(bson.getValue) map (new JavaBsonDate(_))
  }

  implicit val shrinkJavaBsonObjectId: Shrink[JavaBsonObjectId] = Shrink { bson =>
    shrink(bson.getValue) map (new JavaBsonObjectId(_))
  }

  implicit val shrinkJavaBsonArray: Shrink[JavaBsonArray] = Shrink { bson =>
    shrink[Seq[JavaBsonValue]](bson.toSeq) map { values =>
      new JavaBsonArray(java.util.Arrays.asList(values: _*))
    }
  }

  implicit val shrinkJavaBsonDocument: Shrink[JavaBsonDocument] = Shrink { bson =>
    shrink[Map[String, JavaBsonValue]](bson.toMap) map JavaBsonDocument
  }

  implicit val shrinkJavaBsonValue: Shrink[JavaBsonValue] = Shrink {
    case it: JavaBsonDocument  => shrink(it)
    case it: JavaBsonArray     => shrink(it)
    case it: JavaBsonBoolean   => shrink(it)
    case it: JavaBsonInt       => shrink(it)
    case it: JavaBsonLong      => shrink(it)
    case it: JavaBsonNumber    => shrink(it)
    case it: JavaBsonString    => shrink(it)
    case it: JavaBsonObjectId  => shrink(it)
    case it: JavaBsonBinary    => shrink(it)
    case it: JavaBsonDate      => shrink(it)
    case it: JavaBsonRegex     => shrink(it)
    case it: JavaBsonNull      => Stream.empty
    case it: JavaBsonUndefined => Stream.empty
  }

  private def genJavaBson[A: BsonWrites](genValue: Gen[A]): Gen[JavaBsonValue] = for {
    value <- genValue
  } yield {
      val adt = Bson.toBson(value)
      val bson = adt.toJavaBson
      bson
    }

  @inline private def genJavaBson[A: Arbitrary: BsonWrites]: Gen[JavaBsonValue] = genJavaBson(Arbitrary.arbitrary[A])

  def genJavaBsonPrimitive(): Gen[JavaBsonValue] = Gen.oneOf(
    Gen.const(JavaBsonNull),
    genJavaBson[Boolean],
    genJavaBson[Short],
    genJavaBson[Int],
    genJavaBson[Long],
    genJavaBson[Float],
    genJavaBson[Double],
    genJavaBson[String],
    genJavaBson[Array[Byte]],
    genJavaBson[Regex],
    genJavaBson[ObjectId],
    genJavaBson[DateTime]
  )

  def genJavaBsonArray(
    depth: Int = 0,
    genArraySize: Gen[Int] = Gen.choose(0, 5)): Gen[JavaBsonArray] = {
    require(depth >= 0, "depth cannot be negative")
    val genItemValue =
      if (depth == 0) genJavaBsonPrimitive()
      else genJavaBsonArray(depth - 1, genArraySize)
    for {
      n <- genArraySize
      fields <- Gen.listOfN(n, genItemValue)
    } yield new BsonArray(fields)
  }

  def genJavaBsonDocument(
    depth: Int = 0,
    genObjectSize: Gen[Int] = Gen.choose(0, 5)): Gen[JavaBsonDocument] = {
    require(depth >= 0, "depth cannot be negative")
    val genFieldValue =
      if (depth == 0) genJavaBsonPrimitive()
      else genJavaBsonDocument(depth - 1, genObjectSize)
    for {
      n <- genObjectSize
      fields <- Gen.listOfN(n, Gen.zip(Gen.alphaStr.suchThat(!_.isEmpty), genFieldValue))
    } yield JavaBsonDocument(fields)
  }

  def genJavaBsonValue(
    depth: Int = 3,
    genObjectSize: Gen[Int] = Gen.choose(0, 5),
    genArraySize: Gen[Int] = Gen.choose(0, 5)): Gen[JavaBsonValue] = {
    require(depth >= 0, "depth cannot be negative")
    if (depth == 0) genJavaBsonPrimitive()
    else Gen.oneOf(
      genJavaBsonPrimitive(),
      genJavaBsonArray(depth, genObjectSize),
      genJavaBsonDocument(depth, genObjectSize)
    )
  }
}