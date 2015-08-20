package adt.bson

import adt.bson.util.Diff
import org.scalatest.Assertions

trait BsonAssertions extends Assertions {

  def assertEqualBson(actual: BsonValue, expected: BsonValue): Unit = {
    if (actual != expected) {
      val actualPretty = Bson.pretty(actual)
      val expectedPretty = Bson.pretty(expected)
      val diff = Diff.diff(actualPretty, expectedPretty).mkString("\n")
      throw new AssertionError(
        s"\n\n$actualPretty\n\ndid not equal:\n\n$expectedPretty\n\ndiffs:\n\n$diff\n"
      )
    }
  }
}
