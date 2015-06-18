package adt.bson.mongo.codecs

import adt.bson.mongo._
import adt.bson.scalacheck.{BsonValueGenerators, JavaBsonValueGenerators}
import adt.bson.{Bson, BsonValue}
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonCodecImplicitsSpec extends FlatSpec
with GeneratorDrivenPropertyChecks
with BsonCodecImplicits
with BsonValueGenerators
with JavaBsonValueGenerators {

  behavior of "Format[JavaBsonValue]"

  it should "read the same BsonValue that it writes" in {
    forAll() { (original: JavaBsonValue) =>
      val adtBson = Bson.toBson(original)
      val javaBson = Bson.fromBson[JavaBsonValue](adtBson)
      assert(javaBson == original)
    }
  }

  it should "write the same BsonValue that it reads" in {
    forAll() { (original: BsonValue) =>
      val javaBson = Bson.fromBson[JavaBsonValue](original)
      val adtBson = Bson.toBson(javaBson)
      assert(adtBson == original)
    }
  }
}
