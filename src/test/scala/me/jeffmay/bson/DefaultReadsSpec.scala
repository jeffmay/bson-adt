package me.jeffmay.bson

import me.jeffmay.bson.scalacheck.BsonValueGenerators
import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scala.util.matching.Regex

class DefaultReadsSpec extends FlatSpec
with PropertyChecks
with Matchers
with BsonValueGenerators {

  // Slightly more tolerant than the Machine Epsilon, but it'll do
  // See http://en.wikipedia.org/wiki/Machine_epsilon
  val floatEpsilon: Float = 1E-5.toFloat
  val doubleEpsilon: Double = 1E-14

  it should "read Boolean properly" in {
    forAll(genBson[Boolean]) { bson =>
      assert(bson.as[Boolean] == bson.value)
    }
  }

  it should "read Short properly" in {
    forAll(genBson[Short]) { bson =>
      assert(bson.as[Short] == bson.value)
    }
  }

  it should "read Int properly" in {
    forAll(genBson[Int]) { bson =>
      assert(bson.as[Int] == bson.value)
    }
  }

  it should "read Long properly" in {
    forAll(genBson[Long]) { bson =>
      assert(bson.as[Long] == bson.value)
    }
  }

  it should "read Float properly" in {
    forAll { (underlying: Float) =>
      val bson = Bson.toBson(underlying)
      assert(bson.as[Float] === (underlying +- floatEpsilon))
    }
  }

  it should "read Double properly" in {
    forAll { (underlying: Double) =>
      val bson = Bson.toBson(underlying)
      assert(bson.as[Double] === (underlying +- doubleEpsilon))
    }
  }

  it should "read BigDecimal properly" in {
    forAll(genBson[Double]) { bson =>
      assert(BigDecimal(bson.as[Double]) == bson.as[BigDecimal])
    }
  }

  it should "read String properly" in {
    forAll(genBson[String]) { bson =>
      assert(bson.as[String] == bson.value)
    }
  }

  it should "read Binary properly" in {
    forAll(genBson[Array[Byte]]) { bson =>
      assert(bson.as[Array[Byte]] == bson.value)
    }
  }

  it should "read Regex properly" in {
    forAll(genBson[Regex]) { bson =>
      val expected = bson.asInstanceOf[BsonRegex].pattern
      val actual = bson.as[Regex].pattern.pattern()
      assert(actual == expected)
    }
  }

  it should "read ObjectId properly" in {
    forAll(genBson[ObjectId]) { bson =>
      assert(bson.as[ObjectId] == bson.value)
    }
  }

  it should "read ObjectId from String properly" in {
    val oid = new ObjectId()
    val bson = BsonString(oid.toString)
    assert(bson.as[ObjectId] == oid)
  }

  it should "read DateTime in UTC properly" in {
    forAll(genBson[DateTime]) { bson =>
      val expected = bson.asInstanceOf[BsonDate].value withZone DateTimeZone.UTC
      assert(bson.as[DateTime] == expected)
    }
  }

  it should "read Map[String, String] properly" in {
    forAll(genBson[Map[String, String]]) { bson =>
      val expected = bson.asInstanceOf[BsonObject].value mapValues (_.value)
      assert(bson.as[Map[String, String]] == expected)
    }
  }
}
