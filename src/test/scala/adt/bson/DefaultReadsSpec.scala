package adt.bson

import adt.bson.scalacheck.BsonValueGenerators
import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks
import org.scalautils.Tolerance

import scala.util.matching.Regex

class DefaultReadsSpec extends FlatSpec
with PropertyChecks
with Tolerance
with BsonValueGenerators {

  // Slightly more tolerant than the Machine Epsilon, but it'll do
  // See http://en.wikipedia.org/wiki/Machine_epsilon
  val floatEpsilon: Float = 1E-5.toFloat
  val doubleEpsilon: Double = 1E-14

  it should "read Boolean properly" in {
    forAll { (bson: BsonBoolean) =>
      assert(bson.as[Boolean] == bson.value)
    }
  }

  it should "read Short properly" in {
    forAll { (short: Short) =>
      val bson = Bson.toBson(short)
      assert(bson.as[Short] == short)
    }
  }

  it should "read Int properly" in {
    forAll { (bson: BsonInt) =>
      assert(bson.as[Int] == bson.value)
    }
  }

  it should "read Long properly" in {
    forAll { (bson: BsonLong) =>
      assert(bson.as[Long] == bson.value)
    }
  }

  it should "read Float properly" in {
    forAll { (float: Float) =>
      val bson = Bson.toBson(float)
      assert(bson.as[Float] === (float +- floatEpsilon))
    }
  }

  it should "read Double properly" in {
    forAll { (double: Double) =>
      val bson = Bson.toBson(double)
      assert(bson.as[Double] === (double +- doubleEpsilon))
    }
  }

  it should "read BigDecimal properly" in {
    forAll { (bson: BsonNumber) =>
      assert(BigDecimal(bson.as[Double]) == bson.as[BigDecimal])
    }
  }

  it should "read String properly" in {
    forAll { (bson: BsonString) =>
      assert(bson.as[String] == bson.value)
    }
  }

  it should "read Binary properly" in {
    forAll { (bson: BsonBinary) =>
      assert(bson.as[Array[Byte]] == bson.value)
    }
  }

  it should "read Regex properly" in {
    forAll { (bson: BsonRegex) =>
      val expected = bson.pattern
      val actual = bson.as[Regex].pattern.pattern()
      assert(actual == expected)
    }
  }

  it should "read ObjectId properly" in {
    forAll { (bson: BsonObjectId) =>
      assert(bson.as[ObjectId] == bson.value)
    }
  }

  it should "read ObjectId from String properly" in {
    val oid = new ObjectId()
    val bson = BsonString(oid.toString)
    assert(bson.as[ObjectId] == oid)
  }

  it should "read DateTime in UTC properly" in {
    forAll { (bson: BsonDate) =>
      val expected = bson.value withZone DateTimeZone.UTC
      assert(bson.as[DateTime] == expected)
    }
  }

  it should "read Map[String, BsonValue] properly" in {
    forAll { (expected: Map[String, String]) =>
      val bson = Bson.toBson(expected)
      assert(bson.as[Map[String, String]] == expected)
    }
  }
}
