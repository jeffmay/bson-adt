package me.jeffmay.bson

import me.jeffmay.bson.scalacheck.BsonValueGenerators
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class CrossSerializationSpec extends FlatSpec
with Matchers
with PropertyChecks
with BsonValueGenerators {

  it should "write the same unnested Map of Bson values that it reads" in {
    forAll(genBsonObject(depth = 0)) { bson =>
      val map = Bson.fromBson[Map[String, BsonValue]](bson)
      val obj = Bson.toBson(map)
      assert(obj == bson)
    }
  }

  it should "write the same deeply nested Map of Bson values that it reads" in {
    val genNested = for {
      n <- Gen.choose(1, 3)
      obj <- genBsonObject(depth = n, 6 / n)
    } yield obj
    forAll(genNested) { bson =>
      // It should at least be nested twice
      val map = Bson.fromBson[Map[String, Map[String, BsonValue]]](bson)
      val obj = Bson.toBsonObject(map)
      assert(obj == bson)
    }
  }

  it should "write the same unnested Seq of Bson values that it reads" in {
    forAll(genBsonArray(depth = 0)) { bson =>
      val lst = Bson.fromBson[List[BsonValue]](bson)
      val arr = Bson.toBson(lst)
      assert(arr == bson)
    }
  }

  it should "write the same deeply nested List of Bson values that it reads" in {
    val genNested = for {
      n <- Gen.choose(1, 3)
      array <- genBsonArray(depth = n, 6 / n)
    } yield array
    forAll(genNested) { bson =>
      // It should at least be nested twice
      val lst = Bson.fromBson[List[List[BsonValue]]](bson)
      val arr = Bson.toBsonArray(lst)
      assert(arr == bson)
    }
  }
}
