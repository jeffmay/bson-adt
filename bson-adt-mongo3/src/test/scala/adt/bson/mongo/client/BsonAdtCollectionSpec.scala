package adt.bson.mongo.client

import adt.bson.mongo.client.test.TestMongo
import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCollectionSpec extends FlatSpec
with GeneratorDrivenPropertyChecks
with BsonValueGenerators {

  override implicit val generatorDrivenConfig: PropertyCheckConfig = PropertyCheckConfig(
    minSuccessful = 20,
    maxDiscarded = 100
  )

  TestMongo.withDatabase("BsonAdtCodecProviderSpec") { db =>

    it should "write and retrieve a document" in TestMongo.withCollection("test", db) { test =>
      forAll() { (bson: BsonObject) =>
        val id = Bson.obj("_id" -> new ObjectId())
        val doc = bson ++ id
        test.insertOne(doc)
        val found = test.find(id).firstOption
        assert(found === Some(doc))
      }
    }

    it should "find write and retrieve a sequence of documents" in TestMongo.withCollection("test", db) { test =>
      forAll() { (docs: Seq[BsonObject]) =>
        val docsAndIds = docs zip (Stream continually new ObjectId())
        val docsWithIds = docsAndIds.map { case (doc, id) => doc ++ Bson.obj("_id" -> id) }
        whenever(docs.nonEmpty) {
          test.insertMany(docsWithIds)
        }
        val ids = docsAndIds.map(_._2)
        val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
        val found = cursor.toIterable.toVector
        assert(found === docsWithIds)
      }
    }

    it should "bulk insert and retrieve a sequence of documents" in TestMongo.withCollection("test", db) { test =>
      forAll() { (docs: Seq[BsonObject]) =>
        val docsAndIds = docs zip (Stream continually new ObjectId())
        val docsWithIds = docsAndIds.map { case (doc, id) => doc ++ Bson.obj("_id" -> id) }
        val bulkInsert = docsWithIds.map(InsertOneModel(_))
        whenever(docs.nonEmpty) {
          test.bulkWrite(bulkInsert)
        }
        val ids = docsAndIds.map(_._2)
        val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
        val found = cursor.toIterable.toVector
        assert(found == docsWithIds)
      }
    }
  }

}

