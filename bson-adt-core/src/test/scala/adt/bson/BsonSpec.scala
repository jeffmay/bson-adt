package adt.bson

import adt.bson.scalacheck.BsonValueGenerators
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonSpec extends FlatSpec
with GeneratorDrivenPropertyChecks
with BsonValueGenerators {

  "Bson.stringify" should "serialize all BsonValues" in {
    forAll() { (bson: BsonValue) =>
      // TODO: Add Bson.parse for cross serialization?
      val example = Bson.pretty(bson)  // should not throw an exception
//      println(example)  // for checking the output for validity
    }
  }
}
