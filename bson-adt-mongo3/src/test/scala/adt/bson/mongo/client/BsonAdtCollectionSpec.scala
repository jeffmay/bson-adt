package adt.bson.mongo.client

import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCollectionSpec
  extends fixture.FlatSpec
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with BsonValueGenerators {

  override implicit val generatorDrivenConfig: PropertyCheckConfig = PropertyCheckConfig(
    minSuccessful = 20,
    maxDiscarded = 100
  )

  type FixtureParam = BsonAdtCollection

  override protected def withFixture(test: OneArgTest): Outcome = {
    TestMongo.withDatabase("BsonAdtCodecProviderSpec") { db =>
      TestMongo.withCollection("test", db) { collection =>
        test(collection)
      }
    }
  }

  it should "write and retrieve a document" in { test =>
    forAll() { (bson: BsonObject) =>
      val id = Bson.obj("_id" -> new ObjectId())
      val doc = bson ++ id
      test.insertOne(doc)
      val found = test.find(id).headOption
      assert(found === Some(doc))
    }
  }

  it should "find write and retrieve a sequence of documents" in { test =>
    forAll() { (docs: Seq[BsonObject]) =>
      val docsAndIds = docs zip (Stream continually new ObjectId())
      val docsWithIds = docsAndIds.map { case (doc, id) => doc ++ Bson.obj("_id" -> id) }
      whenever(docs.nonEmpty) {
        val result = test.bulkWrite(docsWithIds.map(InsertOneModel(_)))
        assert(result.getInsertedCount === docs.size)
      }
      val ids = docsAndIds.map(_._2)
      val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
      val found = cursor.toVector
      assert(found === docsWithIds)
    }
  }

  it should "bulk insert and retrieve a sequence of documents" in { test =>
    forAll() { (docs: Seq[BsonObject]) =>
      val docsAndIds = docs zip (Stream continually new ObjectId())
      val docsWithIds = docsAndIds.map { case (doc, id) => doc ++ Bson.obj("_id" -> id) }
      val bulkInsert = docsWithIds.map(InsertOneModel(_))
      whenever(docs.nonEmpty) {
        val result = test.bulkWrite(bulkInsert)
        assert(result.getInsertedCount === docs.size)
      }
      val ids = docsAndIds.map(_._2)
      val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
      val foundSorted = cursor.toVector.sortBy(doc => (doc \ "_id").as[ObjectId])
      val expectedSorted = docsWithIds.sortBy(doc => (doc \ "_id").as[ObjectId])
      assert(foundSorted === expectedSorted)
    }
  }
}

