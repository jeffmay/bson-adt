package adt.bson

import org.scalatest.FlatSpec

class BsonValueSpec extends FlatSpec {

  "BsonValue.asOpt" should "not parse an invalid type of Seq" in {
    val bson = Bson.obj(
      "seq" -> Bson.arr(1, 2, 3, "not an int")
    )
    assert(bson.asOpt[Seq[Int]] == None)
  }
}
