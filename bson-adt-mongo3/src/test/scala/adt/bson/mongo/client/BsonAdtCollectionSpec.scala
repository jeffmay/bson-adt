package adt.bson.mongo.client

import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{BsonAssertions, BsonArray, Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCollectionSpec
  extends fixture.FlatSpec
  with BsonAssertions
//  with ParallelTestExecution  // TODO: Figure out how to generate unique test names in parallel better
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with BsonValueGenerators {

  override implicit val generatorDrivenConfig: PropertyCheckConfig = PropertyCheckConfig(
    minSuccessful = 10,
    maxDiscarded = 50,
    maxSize = 10,
    workers = 1
  )

  type FixtureParam = BsonAdtCollection

  override protected def withFixture(test: OneArgTest): Outcome = {
    TestMongo.withDatabase("BsonAdtCollectionSpec") { db =>
      println(s"CREATED DB: ${db.name}")
      val res = TestMongo.withCollection("test", db) { collection =>
        println(s"CREATED COLLECTION: ${collection.namespace}")
        val res = test(collection)
        println(s"DROPPING COLLECTION: ${collection.namespace}")
        res
      }
      println(s"DROPPING COLLECTION: ${db.name}")
      res
    }
  }

  it should "write and retrieve a document" in { test =>
    forAll() { (bson: BsonObject) =>
      val id = Bson.obj("_id" -> new ObjectId())
      val doc = bson ++ id
      test.insertOne(doc)
      val found = test.find(id).headOption
      assert(found.isDefined,
        s"Could not find inserted document in ${test.namespace}:\n${Bson.pretty(doc)}\nusing\n${Bson.inline(id)}")
      assertEqualBson(found.get, doc)
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
      assertEqualBson(BsonArray(found), BsonArray(docsWithIds))
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
      assertEqualBson(BsonArray(foundSorted), BsonArray(expectedSorted))
    }
  }
}

