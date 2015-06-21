package adt.bson.mongo.client.result

import adt.bson.BsonValue

import scala.language.implicitConversions

/**
 * The result of an update operation.  If the update was unacknowledged, then [[acknowledged]] will return false
 * and all other methods with throw [[UnsupportedOperationException]].
 */
class UpdateResult(val underlying: JavaUpdateResult) extends AnyVal {

  /**
   * True if the update was acknowledged.
   */
  def acknowledged: Boolean = underlying.wasAcknowledged()

  /**
   * The number of documents matched
   */
  def matched: Long = underlying.getMatchedCount

  /**
   * If the replace resulted in an inserted document, the _id of the inserted document, otherwise null
   */
  def upsertedId: BsonValue = underlying.getUpsertedId.toBson

  /**
   * The number of documents modified
   */
  def modified: Long = underlying.getModifiedCount

  /**
   * True if the modified count is available
   *
   * @note The modified count is only available when all servers have been upgraded to 2.6 or above.
   */
  def isModifiedCountAvailable: Boolean = underlying.isModifiedCountAvailable
}

object UpdateResult {

  implicit def from(result: JavaUpdateResult): UpdateResult = new UpdateResult(result)
}

/**
 * The result of a delete operation. If the delete was unacknowledged, then [[acknowledged]] will return false
 * and all other methods with throw [[UnsupportedOperationException]].
 */
class DeleteResult(val underlying: JavaDeleteResult) extends AnyVal {

  /**
   * True if the delete was acknowledged.
   */
  def acknowledged: Boolean = underlying.wasAcknowledged()

  /**
   * The number of documents deleted.
   */
  def deleted: Long = underlying.getDeletedCount
}

object DeleteResult {

  implicit def from(result: JavaDeleteResult): DeleteResult = new DeleteResult(result)
}
