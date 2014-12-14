package me.jeffmay.bson.casbah

import java.util.Date

import me.jeffmay.bson._
import me.jeffmay.bson.casbah.CasbahImplicits._
import me.jeffmay.bson.scalacheck.BsonValueGenerators
import org.bson.types.{Binary, ObjectId}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FunSpec
import org.scalatest.prop.PropertyChecks

import scala.util.matching.Regex

class CasbahImplicitsSpec extends FunSpec with PropertyChecks with BsonValueGenerators {

  describe("CasbahImplicits.bsonValue") {

    it("should convert null to BsonNull") {
      assert(bsonValue(null) == BsonNull)
    }

    it("should convert Date to BsonDate in UTC") {
      forAll { (d: Date) =>
        assert(bsonValue(d) == BsonDate(new DateTime(d.getTime, DateTimeZone.UTC)))
      }
    }

    it("should convert Regex to BsonRegex") {
      forAll { (re: Regex) =>
        assert(bsonValue(re) == BsonRegex(re))
      }
    }

    it("should convert java.util.regex.Pattern to BsonRegex") {
      forAll { (pattern: java.util.regex.Pattern) =>
        assert(bsonValue(pattern) == BsonRegex(pattern.pattern.r))
      }
    }

    it("should convert Array[Byte] to BsonBinary") {
      forAll { (bin: Array[Byte]) =>
        assert(bsonValue(bin) == BsonBinary(bin))
      }
    }

    it("should convert Binary to BsonBinary") {
      forAll { (bin: Binary) =>
        assert(bsonValue(bin) == BsonBinary(bin.getData))
      }
    }

    it("should convert MongoDBList to BsonArray") {
      forAll { (obj: BsonArray) =>
        val dbList = obj.toMongoDBList
        assert(bsonValue(dbList) == obj)
      }
    }

    it("should convert MongoDBObject to BsonObject") {
      forAll { (obj: BsonObject) =>
        val dbo = obj.toMongoDBObject
        assert(bsonValue(dbo) == obj)
      }
    }

    it("should convert DBObject to BsonObject") {
      forAll { (obj: BsonObject) =>
        val dbo = obj.toDBObject
        assert(bsonValue(dbo) == obj)
      }
    }

    it("should convert List[Any] to BsonArray") {
      forAll(genBsonArray(depth = 0)) { (bson: BsonArray) =>
        val list = bson.value.map(_.toDBValue).toList
        assert(bsonValue(list) == bson)
      }
    }
  }

  describe("DBValue / BsonValue Conversion") {

    it("should convert BsonObjectId to ObjectId and back") {
      forAll { (oid: ObjectId) =>
        val bson = Bson.toBson(oid)
        val dbVal = bson.toDBValue
        assert(dbVal == oid)
      }
    }

    it("should convert BsonArray to MongoDBList and back without losing the order") {
      forAll { (orig: BsonArray) =>
        val dbList = orig.toMongoDBList
        val arrBson = dbList map bsonValue
        assert(arrBson == orig.as[Seq[BsonValue]])
        val actual = dbList.toBsonArray
        assert(actual == orig)
      }
    }

    it("should convert BsonObject to DBObject and back without losing fields") {
      forAll { (orig: BsonObject) =>
        val dbo = orig.toMongoDBObject
        val mapBson = dbo.mapValues(bsonValue)
        assert(mapBson == orig.as[Map[String, BsonValue]])
        val actual = dbo.toBsonObject
        assert(actual == orig)
      }
    }
  }
}
