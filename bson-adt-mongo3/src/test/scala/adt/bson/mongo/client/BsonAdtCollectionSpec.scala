package adt.bson.mongo.client

// Needed for conversions to mongo
import adt.bson.mongo.client.test.TestMongo
import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCollectionSpec extends FlatSpec
with GeneratorDrivenPropertyChecks
with BsonValueGenerators {

  TestMongo.withDatabase("BsonAdtCodecProviderSpec") { db =>

    class Fixture {
      lazy val test: BsonAdtCollection = db.getBsonCollection("test")
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
        val found = f.test.find(id).firstOption
        assert(found == Some(doc))
      }
    }
  }
}

