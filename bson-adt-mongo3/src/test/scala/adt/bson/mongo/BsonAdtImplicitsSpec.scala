package adt.bson.mongo

import adt.bson.{BsonAdtImplicits, BsonValue}
import adt.bson.scalacheck.{BsonValueGenerators, JavaBsonValueGenerators}
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtImplicitsSpec extends FlatSpec
with BsonAdtImplicits
with GeneratorDrivenPropertyChecks
with JavaBsonValueGenerators
with BsonValueGenerators {

  "BsonValue.toJavaBson" should "write the same value that toBson reads" in {
    forAll() { (adt: BsonValue) =>
      val javaBson = adt.toJavaBson
      val result = javaBson.toBson
      assert(result == adt)
    }
  }

  "BsonValue.toBson" should "write the same value that toJavaBson reads" in {
    forAll() { (bson: JavaBsonValue) =>
      val adtBson = bson.toBson
      val result = adtBson.toJavaBson
      assert(result == bson)
    }
  }
}
