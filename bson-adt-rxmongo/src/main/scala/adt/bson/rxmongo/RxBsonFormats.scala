package adt.bson.rxmongo

import adt.bson._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import reactivemongo.bson.utils.Converters
import reactivemongo.bson._

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * [[BsonFormat]]s for [[BSONValue]]s.
 */
object RxBsonFormats {

  implicit object BSONBooleanFormat extends BsonFormat[BSONBoolean] {
    override def reads(bson: BsonValue): BSONBoolean = BSONBoolean(bson.as[Boolean])
    override def writes(bson: BSONBoolean): BsonValue = BsonBoolean(bson.value)
  }

  implicit object BSONDoubleFormat extends BsonFormat[BSONDouble] {
    override def reads(bson: BsonValue): BSONDouble = BSONDouble(bson.as[Double])
    override def writes(bson: BSONDouble): BsonValue = BsonNumber(bson.value)
  }

  implicit object BSONStringFormat extends BsonFormat[BSONString] {
    override def reads(bson: BsonValue): BSONString = BSONString(bson.as[String])
    override def writes(bson: BSONString): BsonValue = BsonString(bson.value)
  }

  implicit object BSONDateTimeFormat extends BsonFormat[BSONDateTime] {
    override def reads(bson: BsonValue): BSONDateTime = BSONDateTime(bson.as[DateTime].getMillis)
    override def writes(bson: BSONDateTime): BsonValue = BsonDate(bson.value)
  }

  implicit object BSONTimestampFormat extends BsonFormat[BSONTimestamp] {
    override def reads(bson: BsonValue): BSONTimestamp = BSONTimestamp(bson.as[DateTime].getMillis)
    override def writes(bson: BSONTimestamp): BsonValue = BsonDate(bson.value)
  }

  implicit object BSONObjectIDFormat extends BsonFormat[BSONObjectID] {
    override def reads(bson: BsonValue): BSONObjectID = {
      val oid = bson.as[BsonObjectId].value
      BSONObjectID(oid.toByteArray)
    }
    override def writes(bson: BSONObjectID): BsonValue = {
      val oid = new ObjectId(bson.valueAsArray)
      BsonObjectId(oid)
    }
  }

  implicit object BSONRegexFormat extends BsonFormat[BSONRegex] {
    override def reads(bson: BsonValue): BSONRegex = {
      val oid = bson.as[BsonRegex].value
      BSONObjectID(oid.toByteArray)
    }
    override def writes(bson: BSONRegex): BsonValue = {
      BsonObjectId(oid)
    }
  }

  implicit object BSONArrayFormat extends BsonFormat[BSONArray] {
    override def reads(bson: BsonValue): BSONArray = {
      val items = bson.as[BsonArray].value
      val converted = items.toStream.map(toRxBson)
      BSONArray(converted)
    }
    override def writes(bson: BSONArray): BsonValue = {
      val converted = bson.values.map(toBson)
      BsonArray(converted)
    }
  }

  implicit object BSONDocumentFormat extends BsonObjectFormat[BSONDocument] {
    override def reads(bson: BsonValue): BSONDocument = {
      val fields = bson.as[BsonObject].value
      val converted = fields.toStream.map {
        case (k, v) => toRxBson(v).map(k -> _)
      }
      BSONDocument(converted)
    }
    override def writes(bson: BSONDocument): BsonObject = {
      val fields = bson.elements
      val converted = fields.map {
        case (k, v) => k -> toBson(v)
      }.toMap
      BsonObject(converted)
    }
  }




  implicit object BSONRegexBsonFormat extends PartialBsonFormat[BSONRegex] {
    def partialReads: PartialFunction[BsonValue, Try[BSONRegex]] = {
      case js: BsonObject if js.value.size == 1 && js.value.keys.head == "$regex" =>
        js.value.values.head.asOpt[String].
          map(rx => Success(BSONRegex(rx, ""))).
          getOrElse(Failure(__ \ "$regex", "string expected"))
      case obj: BsonObject if obj.value.size == 2 && obj.value.contains("$regex") && obj.value.contains("$options") =>
        val rx = (obj \ "$regex").asOpt[String]
        val opts = (obj \ "$options").asOpt[String]
        (rx, opts) match {
          case (Some(rx), Some(opts)) => Success(BSONRegex(rx, opts))
          case (None, Some(_))        => Failure(__ \ "$regex", "string expected")
          case (Some(_), None)        => Failure(__ \ "$options", "string expected")
          case _                      => Failure(__ \ "$regex", "string expected") ++ Failure(__ \ "$options", "string expected")
        }
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case rx: BSONRegex =>
        if (rx.flags.isEmpty())
          Bson.obj("$regex" -> rx.value)
        else Bson.obj("$regex" -> rx.value, "$options" -> rx.flags)
    }
  }
  implicit object BSONNullBsonFormat extends PartialBsonFormat[BSONNull.type] {
    def partialReads: PartialFunction[BsonValue, Try[BSONNull.type]] = {
      case BsonNull => Success(BSONNull)
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case BSONNull => BsonNull
    }
  }
  implicit object BSONUndefinedBsonFormat extends PartialBsonFormat[BSONUndefined.type] {
    def partialReads: PartialFunction[BsonValue, Try[BSONUndefined.type]] = {
      case _: BsonUndefined => Success(BSONUndefined)
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case BSONUndefined => BsonUndefined("")
    }
  }
  implicit object BSONIntegerBsonFormat extends PartialBsonFormat[BSONInteger] {
    def partialReads: PartialFunction[BsonValue, Try[BSONInteger]] = {
      case BsonObject(("$int", BsonNumber(i)) +: Nil) => Success(BSONInteger(i.toInt))
      case BsonNumber(i)                            => Success(BSONInteger(i.toInt))
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case int: BSONInteger => BsonNumber(int.value)
    }
  }
  implicit object BSONLongBsonFormat extends PartialBsonFormat[BSONLong] {
    def partialReads: PartialFunction[BsonValue, Try[BSONLong]] = {
      case BsonObject(("$long", BsonNumber(long)) +: Nil) => Success(BSONLong(long.toLong))
      case BsonNumber(long)                             => Success(BSONLong(long.toLong))
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case long: BSONLong => BsonNumber(long.value)
    }
  }
  implicit object BSONBinaryBsonFormat extends PartialBsonFormat[BSONBinary] {
    def partialReads: PartialFunction[BsonValue, Try[BSONBinary]] = {
      case BsonString(str) => try {
        Success(BSONBinary(Converters.str2Hex(str), Subtype.UserDefinedSubtype))
      } catch {
        case NonFatal(e) => Failure(new IllegalArgumentException(s"error deserializing hex ${e.getMessage}"))
      }
      case obj: BsonObject if obj.value.exists {
        case (str, _: BsonString) if str == "$binary" => true
        case _                                      => false
      } => try {
        Success(BSONBinary(Converters.str2Hex((obj \ "$binary").as[String]), Subtype.UserDefinedSubtype))
      } catch {
        case NonFatal(e) => Failure(new IllegalArgumentException(s"error deserializing hex ${e.getMessage}"))
      }
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case binary: BSONBinary => {
        val remaining = binary.value.readable
        Bson.obj(
          "$binary" -> Converters.hex2Str(binary.value.slice(remaining).readArray(remaining)),
          "$type" -> Converters.hex2Str(Array(binary.subtype.value.toByte)))
      }
    }
  }
  implicit object BSONSymbolBsonFormat extends PartialBsonFormat[BSONSymbol] {
    def partialReads: PartialFunction[BsonValue, Try[BSONSymbol]] = {
      case BsonObject(("$symbol", BsonString(v)) +: Nil) => Success(BSONSymbol(v))
    }
    val partialWrites: PartialFunction[BSONValue, BsonValue] = {
      case BSONSymbol(s) => Bson.obj("$symbol" -> s)
    }
  }

  def toRxBson(json: BsonValue): Try[BSONValue] = {
    BSONStringBsonFormat.partialReads.
      orElse(BSONObjectIDBsonFormat.partialReads).
      orElse(BSONDateTimeBsonFormat.partialReads).
      orElse(BSONTimestampBsonFormat.partialReads).
      orElse(BSONBinaryBsonFormat.partialReads).
      orElse(BSONRegexBsonFormat.partialReads).
      orElse(BSONDoubleBsonFormat.partialReads).
      orElse(BSONIntegerBsonFormat.partialReads).
      orElse(BSONLongBsonFormat.partialReads).
      orElse(BSONBooleanBsonFormat.partialReads).
      orElse(BSONNullBsonFormat.partialReads).
      orElse(BSONUndefinedBsonFormat.partialReads).
      orElse(BSONSymbolBsonFormat.partialReads).
      orElse(BSONArrayBsonFormat.partialReads).
      orElse(BSONDocumentBsonFormat.partialReads).
      lift(json).getOrElse(Failure(s"unhandled json value: $json"))
  }

  def toBson(bson: BSONValue): BsonValue = BSONObjectIDBsonFormat.partialWrites.
    orElse(BSONDateTimeBsonFormat.partialWrites).
    orElse(BSONTimestampBsonFormat.partialWrites).
    orElse(BSONBinaryBsonFormat.partialWrites).
    orElse(BSONRegexBsonFormat.partialWrites).
    orElse(BSONDoubleBsonFormat.partialWrites).
    orElse(BSONIntegerBsonFormat.partialWrites).
    orElse(BSONLongBsonFormat.partialWrites).
    orElse(BSONBooleanBsonFormat.partialWrites).
    orElse(BSONNullBsonFormat.partialWrites).
    orElse(BSONUndefinedBsonFormat.partialWrites).
    orElse(BSONStringBsonFormat.partialWrites).
    orElse(BSONSymbolBsonFormat.partialWrites).
    orElse(BSONArrayBsonFormat.partialWrites).
    orElse(BSONDocumentBsonFormat.partialWrites).
    lift(bson).getOrElse(throw new RuntimeException(s"unhandled json value: $bson"))
}

object Writers {
  implicit class JsPathMongo(val jp: JsPath) extends AnyVal {
    def writemongo[A](implicit writer: BsonWrites[A]): BsonObjectWrites[A] = {
      BsonObjectWrites[A] { (o: A) =>
        val newPath = jp.path.flatMap {
          case e: KeyPathNode     => Some(e.key)
          case e: RecursiveSearch => Some(s"$$.${e.key}")
          case e: IdxPathNode     => Some(s"${e.idx}")
        }.mkString(".")

        val orig = writer.writes(o)
        orig match {
          case BsonObject(e) =>
            BsonObject(e.flatMap {
              case (k, v) => Seq(s"${newPath}.${k}" -> v)
            })
          case e: BsonValue => BsonObject(Seq(newPath -> e))
        }
      }
    }
  }
}