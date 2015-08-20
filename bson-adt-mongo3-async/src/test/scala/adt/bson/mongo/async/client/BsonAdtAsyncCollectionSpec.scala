package adt.bson.mongo.async.client

import adt.bson.mongo.client.model.InsertOneModel
import adt.bson.scalacheck.BsonValueGenerators
import adt.bson.{BsonArray, BsonAssertions, Bson, BsonObject}
import org.bson.types.ObjectId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ParallelTestExecution, Outcome, fixture}

import scala.concurrent.duration._

class BsonAdtAsyncCollectionSpec
  extends fixture.FlatSpec
  with BsonAssertions
//  with ParallelTestExecution  // TODO: Figure out how to generate unique test names better
  with ScalaFutures
  with GeneratorDrivenPropertyChecks
  with BsonValueGenerators {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(1.second), interval = scaled(50.milliseconds))

  override implicit val generatorDrivenConfig: PropertyCheckConfig = PropertyCheckConfig(
    minSuccessful = 10,
    maxDiscarded = 50,
    maxSize = 3,
    workers = 1
  )

  override type FixtureParam = BsonAdtAsyncCollection

  override protected def withFixture(test: OneArgTest): Outcome = {
    TestMongo.withDatabase("BsonAdtAsyncCollectionSpec") { db =>
      println(s"CREATED DB: ${db.name}")
      val res = TestMongo.withCollection("test", db) { collection =>
        println(s"CREATED COLLECTION: ${collection.namespace}")
        val res = test(collection)
        println(s"DROPPING COLLECTION: ${collection.namespace}")
        res
      }
      println(s"DROPPING DB: ${db.name}")
      res
    }
  }

  it should "write and retrieve a document" in { test =>
    forAll() { (bson: BsonObject) =>
      val id = Bson.obj("_id" -> new ObjectId())
      val doc = bson ++ id
      val () = test.insertOne(doc).futureValue
      val found = test.find(id).first().futureValue
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
        val result = test.bulkWrite(docsWithIds.map(InsertOneModel(_))).futureValue
        assert(result.asAcknowledged.inserted === docs.size)
      }
      val ids = docsAndIds.map(_._2)
      val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
      val found = cursor.sequence().futureValue
      assertEqualBson(BsonArray(found), BsonArray(docsWithIds))
    }
  }

  it should "bulk insert and retrieve a sequence of documents" in { test =>
    forAll() { (docs: Seq[BsonObject]) =>
      val docsAndIds = docs zip (Stream continually new ObjectId())
      val docsWithIds = docsAndIds.map { case (doc, id) => doc ++ Bson.obj("_id" -> id) }
      val bulkInsert = docsWithIds.map(InsertOneModel(_))
      whenever(docs.nonEmpty) {
        val result = test.bulkWrite(bulkInsert).futureValue
        assert(result.asAcknowledged.inserted === docs.size)
      }
      val ids = docsAndIds.map(_._2)
      val cursor = test.find(Bson.obj("_id" -> Bson.obj("$in" -> ids)))
      val found = cursor.sequence().futureValue
      assertEqualBson(BsonArray(found), BsonArray(docsWithIds))
    }
  }
}
