package adt.bson.util

import org.google.plaintext.diff_match_patch

import scala.collection.JavaConversions.iterableAsScalaIterable

object Diff {

  private val differ = new diff_match_patch

  object Op extends Enumeration {
    val Delete = Value("DELETE")
    val Insert = Value("INSERT")
    val Equal = Value("EQUAL")
  }

  def diff(left: String, right: String): Seq[Diff] = {
    val diffs = differ.diff_main(left, right)
    differ.diff_cleanupSemantic(diffs)
    diffs.map(d => Diff(d.text, Diff.Op.withName(d.operation.name()))).toSeq
  }

//  def byLine(diffs: Seq[Diff]): Seq[(String, String)] = {
//    println(diffs)
//    Seq.empty
//  }
}

case class Diff(text: String, op: Diff.Op.Value)