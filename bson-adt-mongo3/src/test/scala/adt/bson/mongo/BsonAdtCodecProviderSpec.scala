package adt.bson.mongo

import adt.bson.mongo.client.test.TestMongo
import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCodecProviderSpec extends FlatSpec
with GeneratorDrivenPropertyChecks
with BsonValueGenerators {

  TestMongo.withDatabase("BsonAdtCodecProviderSpec") { db =>

    class Fixture {
      lazy val test = db.getCollection("test", classOf[BsonObject])
    }

    def withFixture(f: Fixture => Unit): Unit = {
      val fixture = new Fixture
      f(fixture)
      fixture.test.drop()
    }

    it should "find the same document it writes" in withFixture { f =>
      forAll() { (bson: BsonObject) =>
        val id = Bson.obj("_id" -> new ObjectId())
        val doc = bson ++ id
        f.test.insertOne(doc)
        val found = f.test.find(id).iterator().next()
        assert(found == doc)
      }
    }
  }
}

