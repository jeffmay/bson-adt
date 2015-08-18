package adt.bson.mongo.bulk

import scala.language.implicitConversions
import scala.util.Try
import scala.collection.JavaConversions.iterableAsScalaIterable

/**
 * The result of a successful bulk write operation (substitutes [[com.mongodb.bulk.BulkWriteResult]]).
 */
sealed trait BulkWriteResult {

  /**
   * True if the write was acknowledged.
   */
  def isAcknowledged: Boolean

  /**
   * @return this as Some [[AcknowledgedBulkWriteResult]] or None.
   */
  def acknowledged: Option[AcknowledgedBulkWriteResult] = if (isAcknowledged) Some(asAcknowledged) else None

  /**
   * Converts this result to an acknowledged
   */
  @throws[IllegalStateException]("If the write was not acknowledged")
  def asAcknowledged: AcknowledgedBulkWriteResult =
    if (isAcknowledged) this.asInstanceOf[AcknowledgedBulkWriteResult]
    else throw new IllegalStateException("The write was not acknowledged")
}
object BulkWriteResult {

  implicit def from(result: JavaBulkWriteResult): BulkWriteResult = {
    if (result.wasAcknowledged())
      AcknowledgedBulkWriteResult(
        result.getInsertedCount,
        result.getMatchedCount,
        result.getDeletedCount,
        Try(result.getModifiedCount),
        result.getUpserts.toSeq.map(BulkWriteUpsert.from)
      )
    else UnacknowledgedBulkWriteResult
  }
}

/**
 * Indicates a write was unacknowledged (substitutes [[com.mongodb.bulk.BulkWriteResult.unacknowledged]]).
 */
case object UnacknowledgedBulkWriteResult extends BulkWriteResult {
  override def isAcknowledged: Boolean = false
}

/**
 * The result of a successful bulk write operation (substitutes [[com.mongodb.bulk.BulkWriteResult.acknowledged]]).
 *
 * @param inserted the number of documents inserted by the write operation
 * @param matched the number of documents matched by the write operation
 * @param deleted the number of documents removed by the write operation
 * @param _modified an attempt to get the modified count from the result
 * @param upserts the list of upserts
 */
case class AcknowledgedBulkWriteResult(
  inserted: Int,
  matched: Int,
  deleted: Int,
  private val _modified: Try[Int],
  upserts: Seq[BulkWriteUpsert]
  ) extends BulkWriteResult {

  override def isAcknowledged: Boolean = true

  /**
   * The number of documents modified, which may not be available, if the server was not able to provide the count
   */
  def modified: Int = _modified.get

  /**
   * Returns true if the server was able to provide a count of modified documents.
   * If this method returns false (which can happen if the server is not at least version
   * 2.6) then the [[modified]] method will throw [[UnsupportedOperationException}]].
   */
  def isModifiedCountAvailable: Boolean = _modified.isSuccess
}