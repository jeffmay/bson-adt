package adt.bson

import org.scalatest.FlatSpec

class BsonValueSpec extends FlatSpec {

  "BsonValue.asOpt" should "something" in {
    val bson = Bson.obj(
      "seq" -> Bson.arr(1, 2, 3, "not an int")
    )
    val ints = bson.asOpt[Seq[Int]]
  }
}
