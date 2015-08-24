package adt.bson.scalacheck

import org.bson.types.Binary
import org.joda.time.DateTime
import org.scalacheck.ops.time.joda.ImplicitJodaTimeGenerators
import org.scalacheck.{Arbitrary, Shrink}

trait CommonGenerators extends ImplicitJodaTimeGenerators {

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
}
