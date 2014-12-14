package me.jeffmay.bson

import scala.language.implicitConversions

/**
 * A combined Bson serializer / deserializer that always serializes [[BsonObject]]s.
 *
 * This is different from [[BsonFormat]] in that it guarantees that the Bson
 * produced will be an object and not a primitive value. Since a [[BsonObject]] is
 * required to store a document in the database, this is a type-safe way to insure
 * that the resulting Bson will be suitable as a document.
 */
trait BsonObjectFormat[A] extends BsonFormat[A] with BsonObjectWrites[A]

object BsonObjectFormat {

  implicit def apply[A](implicit reader: BsonReads[A], writer: BsonObjectWrites[A]): BsonObjectFormat[A] = {
    new BsonObjectFormat[A] {
      override def reads(bson: BsonValue): A = reader.reads(bson)
      override def writes(model: A): BsonObject = writer.writes(model)
    }
  }

  def of[A: BsonObjectFormat]: BsonObjectFormat[A] = implicitly
}

/**
 * A combined Bson serializer / deserializer for any type of value.
 */
trait BsonFormat[A] extends BsonReads[A] with BsonWrites[A]

object BsonFormat {

  implicit def apply[A](implicit reader: BsonReads[A], writer: BsonWrites[A]): BsonFormat[A] = {
    new BsonFormat[A] {
      override def reads(bson: BsonValue): A = reader.reads(bson)
      override def writes(value: A): BsonValue = writer.writes(value)
    }
  }

  def of[A: BsonFormat]: BsonFormat[A] = implicitly
}