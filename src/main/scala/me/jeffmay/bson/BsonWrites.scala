package me.jeffmay.bson

import org.bson.types.ObjectId
import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.functional.ContravariantFunctor

import scala.annotation.implicitNotFound
import scala.language.{higherKinds, implicitConversions}
import scala.util.matching.Regex

/**
 * BsonObject serializer: write an implicit to define an object / document serializer for any Scala type
 */
@implicitNotFound("No Bson serializer as BsonObject found for type ${A}. " +
  "Implement or import an implicit BsonObjectWrites or BsonObjectFormat for this type."
)
trait BsonObjectWrites[-A] extends BsonWrites[A] {

  /**
   * Convert the object into a [[BsonObject]]
   */
  override def writes(model: A): BsonObject

  /**
   * A writer that prunes null fields from the resulting [[BsonObject]].
   */
  override def prune: BsonObjectWrites[A] = {
    BsonObjectWrites[A] { a =>
      writes(a).pruned
    }
  }
}

object BsonObjectWrites {

  def apply[A](f: A => BsonObject): BsonObjectWrites[A] = new BsonObjectWrites[A] {
    override def writes(model: A): BsonObject = f(model)
  }

  def of[A: BsonObjectWrites]: BsonObjectWrites[A] = implicitly

  implicit object BsonWritesContravariantFunctor extends ContravariantFunctor[BsonObjectWrites] {
    def contramap[A, B](aWriter: BsonObjectWrites[A], f: B => A): BsonObjectWrites[B] = {
      BsonObjectWrites[B] { b =>
        val a = f(b)
        aWriter writes a
      }
    }
  }
}

/**
 * Bson serializer: write an implicit to define a [[BsonValue]] serializer for any Scala type
 */
@implicitNotFound("No Bson serializer found for type ${A}. " +
  "Implement or import an implicit BsonWrites or BsonFormat for this type."
)
trait BsonWrites[-A] {

  /**
   * Convert the object into a [[BsonValue]]
   */
  def writes(value: A): BsonValue

  /**
   * Transform the resulting [[BsonValue]] using given function
   */
  def transform(transformer: BsonValue => BsonValue): BsonWrites[A] = {
    BsonWrites[A] { a =>
      val bson = this writes a
      transformer(bson)
    }
  }

  /**
   * Transform the resulting [[BsonValue]] using another writer
   */
  def transform(transformer: BsonWrites[BsonValue]): BsonWrites[A] = {
    BsonWrites[A] { a =>
      val bson = this writes a
      transformer writes bson
    }
  }

  /**
   * A writer that prunes null fields from the resulting [[BsonValue]].
   */
  def prune: BsonWrites[A] = transform(_.pruned)
}

object BsonWrites extends DefaultWrites {

  def apply[A](f: A => BsonValue): BsonWrites[A] = new BsonWrites[A] {
    override def writes(value: A): BsonValue = f(value)
  }

  def of[A: BsonWrites]: BsonWrites[A] = implicitly
}

/**
 * Default Bson Serializers.
 */
trait DefaultWrites {

  implicit object BsonWritesBsonValue extends BsonWrites[BsonValue] {
    override def writes(value: BsonValue): BsonValue = value
  }

  implicit object BsonWritesBoolean extends BsonWrites[Boolean] {
    override def writes(value: Boolean): BsonValue = BsonBoolean(value)
  }

  implicit object BsonWritesString extends BsonWrites[String] {
    override def writes(value: String): BsonValue = BsonString(value)
  }

  implicit object BsonWritesBinary extends BsonWrites[Array[Byte]] {
    override def writes(value: Array[Byte]): BsonValue = BsonBinary(value)
  }

  implicit object BsonWritesRegex extends BsonWrites[Regex] {
    override def writes(value: Regex): BsonValue = BsonRegex(value)
  }

  implicit object BsonWritesDateTime extends BsonWrites[DateTime] {
    // Mongo only stores Dates in UTC
    override def writes(value: DateTime): BsonValue = BsonDate(value withZone DateTimeZone.UTC)
  }

  implicit object BsonWritesObjectId extends BsonWrites[ObjectId] {
    override def writes(value: ObjectId): BsonValue = BsonObjectId(value)
  }

  implicit object BsonWritesShort extends BsonWrites[Short] {
    override def writes(value: Short): BsonValue = BsonInt(value)
  }

  implicit object BsonWritesInt extends BsonWrites[Int] {
    override def writes(value: Int): BsonValue = BsonInt(value)
  }

  implicit object BsonWritesLong extends BsonWrites[Long] {
    override def writes(value: Long): BsonValue = BsonLong(value)
  }

  implicit object BsonWritesFloat extends BsonWrites[Float] {
    override def writes(value: Float): BsonValue = BsonNumber(value)
  }

  implicit object BsonWritesDouble extends BsonWrites[Double] {
    override def writes(value: Double): BsonValue = BsonNumber(value)
  }

  implicit def optionWrites[T](implicit writer: BsonWrites[T]): BsonWrites[Option[T]] = {
    new BsonWrites[Option[T]] {
      override def writes(value: Option[T]): BsonValue = value match {
        case Some(writeable) => writer writes writeable
        case None => BsonNull
      }
    }
  }

  implicit def mapObjectWrites[V](implicit writer: BsonWrites[V]): BsonObjectWrites[Map[String, V]] = {
    new BsonObjectWrites[Map[String, V]] {
      override def writes(valueMap: Map[String, V]): BsonObject = {
        val bsonMap = valueMap mapValues writer.writes
        BsonObject(bsonMap)
      }
    }
  }

  implicit def traversableWrites[T](implicit writer: BsonWrites[T]): BsonWrites[Traversable[T]] = {
    new BsonWrites[Traversable[T]] {
      override def writes(values: Traversable[T]): BsonValue = {
        BsonArray(values.map(writer.writes).toSeq)
      }
    }
  }
}