package adt.bson

import java.util

import adt.bson.scalacheck.BsonValueGenerators
import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
import org.scalactic.Tolerance
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

import scala.util.matching.Regex

class DefaultWritesSpec extends FlatSpec
with PropertyChecks
with Tolerance
with BsonValueGenerators {

  // Slightly more tolerant than the Machine Epsilon, but it'll do
  // See http://en.wikipedia.org/wiki/Machine_epsilon
  val floatEpsilon: Float = 1E-5.toFloat
  val doubleEpsilon: Double = 1E-14

  it should "write Boolean properly" in {
    forAll { (value: Boolean) =>
      assert(Bson.toBson(value) == BsonBoolean(value))
    }
  }

  it should "write Short properly" in {
    forAll { (value: Short) =>
      assert(Bson.toBson(value) == BsonInt(value))
    }
  }

  it should "write Int properly" in {
    forAll { (value: Int) =>
      assert(Bson.toBson(value) == BsonInt(value))
    }
  }

  it should "write Long properly" in {
    forAll { (value: Long) =>
      assert(Bson.toBson(value) == BsonLong(value))
    }
  }

  it should "write Float properly" in {
    forAll { (value: Float) =>
      val underlying = Bson.toBson(value).value.asInstanceOf[Double]
      assert(underlying.toFloat === (value +- floatEpsilon))
    }
  }

  it should "write Double properly" in {
    forAll { (value: Double) =>
      val underlying = Bson.toBson(value).value.asInstanceOf[Double]
      assert(underlying === (value +- doubleEpsilon))
    }
  }

  it should "write String properly" in {
    forAll { (value: String) =>
      assert(Bson.toBson(value) == BsonString(value))
    }
  }

  it should "write Binary properly" in {
    forAll { (value: Array[Byte]) =>
      val clone = util.Arrays.copyOf(value, value.length)
      assert(Bson.toBson(value) == BsonBinary(clone))
    }
  }

  it should "write Regex properly" in {
    forAll { (value: Regex) =>
      val clone = value.pattern.pattern.r
      assert(Bson.toBson(value) == BsonRegex(clone))
    }
  }

  it should "write ObjectId properly" in {
    forAll { (value: ObjectId) =>
      val clone = new ObjectId(value.toByteArray)
      assert(Bson.toBson(value) == BsonObjectId(clone))
    }
  }

  it should "write DateTime properly in UTC" in {
    forAll { (value: DateTime) =>
      assert(Bson.toBson(value) == BsonDate(value withZone DateTimeZone.UTC))
    }
  }

  it should "write Map[String, String] properly" in {
    forAll { (fields: Seq[(String, String)]) =>
      val expected = BsonObject(fields.toMap mapValues BsonString)
      assert(Bson.toBson(fields.toMap) == expected)
    }
  }

}
