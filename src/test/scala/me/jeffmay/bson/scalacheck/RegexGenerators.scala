package me.jeffmay.bson.scalacheck

import java.util.regex.Pattern

import org.scalacheck.{Arbitrary, Gen}

import scala.util.Try
import scala.util.matching.Regex

trait RegexGenerators {

  val genRegexChar: Gen[Char] = Gen.choose(' ', '~')  // any ascii value

  implicit val arbRegex: Arbitrary[Regex] = Arbitrary {
    Gen.listOf(genRegexChar)
      .map(chrs => Try(chrs.mkString.r))
      .retryUntil(_.isSuccess)
      .map(_.get)
  }

  implicit val arbPattern: Arbitrary[java.util.regex.Pattern] = Arbitrary {
    Gen.listOf(genRegexChar)
      .map(chrs => Try(Pattern.compile(chrs.mkString)))
      .retryUntil(_.isSuccess)
      .map(_.get)
  }
}
