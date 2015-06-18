package adt.bson.scalacheck

import org.bson.types.{Binary, ObjectId}
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.scalacheck.{Arbitrary, Gen, Shrink}

trait CommonGenerators {

  // TODO: Upgrade ScalaCheckOps to get these
  implicit def arbitraryDateTimeZone: Arbitrary[DateTimeZone] = Arbitrary {
    for {
      id <- Gen.oneOf(DateTimeZone.getAvailableIDs.toArray[String](Array.empty).toSeq)
    } yield DateTimeZone.forID(id)
  }

  implicit def arbitraryDateTime(implicit genDateTimeZone: Arbitrary[DateTimeZone]): Arbitrary[DateTime] = {
    Arbitrary(
      for {
        millis <- Gen.choose(0L, Long.MaxValue)
        dateTimeZone <- genDateTimeZone.arbitrary
      } yield new DateTime(millis, dateTimeZone)
    )
  }

  implicit def arbitraryLocalDateTime: Arbitrary[LocalDateTime] = {
    Arbitrary(
      for {
        millis <- Gen.choose(0L, Long.MaxValue)
      } yield new LocalDateTime(millis)
    )
  }

  protected def halves[T](n: T)(implicit integral: Integral[T]): Stream[T] = {
    import integral._
    if (n == zero) Stream.empty
    else n #:: halves(n / fromInt(2))
  }

  protected def shrinkSameSign[T](value: T)(implicit integral: Integral[T]): Stream[T] = {
    import integral._
    zero #:: halves(value).map(value - _)
  }

  implicit val shrinkDateTime: Shrink[DateTime] = Shrink { datetime =>
    val shrinkMillis = shrinkSameSign(datetime.getMillis)
    shrinkMillis map { new DateTime(_, datetime.getZone) }
  }

  implicit def arbBinary(implicit arbBytes: Arbitrary[Array[Byte]]): Arbitrary[Binary] = Arbitrary {
    arbBytes.arbitrary.map(new Binary(_))
  }

  implicit def arbObjectId(implicit arbDate: Arbitrary[java.util.Date]): Arbitrary[ObjectId] = Arbitrary {
    for {
      date <- arbDate.arbitrary
    } yield new ObjectId(date)
  }

  // The only way in which the ObjectId could cause a property to fail is if someone is using
  // the timestamp from the ObjectId in a test. So we only need to shrink the timestamp.
  implicit val shrinkObjectId: Shrink[ObjectId] = Shrink { oid =>
    val shrinkSeconds = shrinkSameSign(oid.getTimestamp)
    shrinkSeconds map { millis => new ObjectId(new java.util.Date(millis * 1000)) }
  }
}
