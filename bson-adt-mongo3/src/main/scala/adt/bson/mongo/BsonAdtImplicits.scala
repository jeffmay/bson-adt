package adt.bson.mongo

import adt.bson._
import adt.bson.mongo.BsonAdtImplicits.{JavaBsonDocumentOps, JavaBsonValueOps, BsonAdtObjectOps, BsonAdtValueOps}
import adt.bson.mongo.codecs.BsonAdtCodec
import org.bson.{BsonDocumentReader, BsonDocumentWriter}
import org.bson.codecs.{DecoderContext, EncoderContext}
import org.bson.codecs.configuration.CodecRegistry

import scala.language.implicitConversions

/**
 * Adds implicit conversions between [[JavaBsonValue]] and [[BsonValue]].
 */
trait BsonAdtImplicits {

  implicit def bsonValueAsJavaOps(adt: BsonValue): BsonAdtValueOps = new BsonAdtValueOps(adt)
  implicit def bsonObjectAsJavaOps(adt: BsonObject): BsonAdtObjectOps = new BsonAdtObjectOps(adt)

  implicit def bsonValueAsAdt(bson: JavaBsonValue): JavaBsonValueOps = new JavaBsonValueOps(bson)
  implicit def bsonDocumentAsAdt(doc: JavaBsonDocument): JavaBsonDocumentOps = new JavaBsonDocumentOps(doc)

  // Adds org.bson.conversions.Bson without using inheritence
  implicit def asBsonConversion(adt: BsonObject): org.bson.conversions.Bson = {
    new org.bson.conversions.Bson {
      override def toBsonDocument[TDocument](documentClass: Class[TDocument], codecRegistry: CodecRegistry): JavaBsonDocument = {
        adt.toJavaBsonDocument
      }
    }
  }

}

object BsonAdtImplicits extends BsonAdtImplicits {

  class BsonAdtValueOps(val adt: BsonValue) extends AnyVal {

    def toJavaBson: JavaBsonValue = {
      adt match {
        case obj: BsonObject => obj.toJavaBsonDocument
        case _ =>
          val doc = Bson.obj("obj" -> adt).toJavaBsonDocument
          doc.get("obj")
      }
    }
  }

  class BsonAdtObjectOps(val doc: BsonObject) extends AnyVal {

    def toJavaBsonDocument: JavaBsonDocument = {
      val writer = new BsonDocumentWriter(new JavaBsonDocument())
      BsonAdtCodec.encode(writer, doc, EncoderContext.builder().build())
      writer.getDocument
    }
  }

  class JavaBsonValueOps(val bson: JavaBsonValue) extends AnyVal {

    def toBson: BsonValue = bson match {
      case doc: JavaBsonDocument => doc.toBsonObject
      case _ =>
        val obj = JavaBsonDocument(Seq("obj" -> bson)).toBsonObject
        obj \ "obj"
    }
  }
  
  class JavaBsonDocumentOps(val doc: JavaBsonDocument) extends AnyVal {
    
    def toBsonObject: BsonObject = {
      val reader = new BsonDocumentReader(doc)
      val adt = BsonAdtCodec.decode(reader, DecoderContext.builder().build())
      adt.as[BsonObject]
    }
  }
}
