package adt.bson.mongo.codecs

import adt.bson.{BsonObject, BsonValue}
import adt.bson.mongo._
import adt.bson.scalacheck._
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonDocument, BsonDocumentReader, BsonDocumentWriter}
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BsonAdtCodecSpec extends FlatSpec
with BsonCodecImplicits
with GeneratorDrivenPropertyChecks
with BsonValueGenerators
with JavaBsonValueGenerators
with RegexGenerators {

  behavior of "Codec[BsonValue]"

  def codec: Codec[BsonValue] = implicitly[Codec[BsonValue]]

  it should "write the same BsonDocument that it reads" in {
    forAll() { (doc: JavaBsonDocument) =>
      val reader = new BsonDocumentReader(doc)
      val adt = codec.decode(reader, DecoderContext.builder().build())
      val writer = new BsonDocumentWriter(new BsonDocument())
      codec.encode(writer, adt, EncoderContext.builder().build())
      val result = writer.getDocument
      assert(result equals doc)
    }
  }

  it should "read the same BsonObject that it writes" in {
    forAll() { (adt: BsonObject) =>
      val writer = new BsonDocumentWriter(new BsonDocument())
      codec.encode(writer, adt, EncoderContext.builder().build())
      val doc = writer.getDocument
      val reader = new BsonDocumentReader(doc)
      val result = codec.decode(reader, DecoderContext.builder().build())
      assert(result equals adt)
    }
  }
}
