package adt.bson.mongo.bulk

import adt.bson.BsonObject

import scala.concurrent.duration.FiniteDuration

/**
 * The settings to apply to the creation of an index (substitutes [[com.mongodb.bulk.IndexRequest]]).
 *
 * @param keys The index keys
 * @param name The name of the index
 * @param background True if should create the index in the background
 * @param sparse If true, the index only references documents with the specified field
 * @param unique True if the index should be unique
 * @param version The index version number
 * @param expiresAfter The time to live for documents in the collection
 * @param defaultLanguage The language for a text index.  The language that determines the list of stop words and
 *                        the rules for the stemmer and tokenizer.
 * @param languageOverride The name of the field that contains the language string.  For text indexes, the name
 *                         of the field, in the collection's documents, that contains the override language for
 *                         the document.
 * @param textVersion The text index version number.
 * @param weights the weighting object for use with a text index
 *               A document that represents field and weight pairs. The weight is an integer ranging from 1 to 99,999
 *               and denotes the significance of the field relative to the other indexed fields in terms of the score.
 * @param bits The number of precision of the stored geohash value of the location data in 2d indexes
 * @param min The lower inclusive boundary for the longitude and latitude values for 2d indexes
 * @param max The upper inclusive boundary for the longitude and latitude values for 2d indexes
 * @param bucketSize the specified the number of units within which to group the location values for geoHaystack Indexes
 * @param sphereVersion The 2dsphere index version number
 * @param dropDups The legacy dropDups setting
 *                 Prior to MongoDB 3.0 dropDups could be used with unique indexes allowing documents with
 *                 duplicate values to be dropped when building the index. Later versions of MongoDB
 *                 will silently ignore this setting.
 */
case class IndexRequest(
  keys: BsonObject,
  name: Option[String] = None,
  background: Boolean = false,
  sparse: Boolean = false,
  unique: Boolean = false,
  version: Option[Int] = None,
  expiresAfter: Option[FiniteDuration] = None,
  defaultLanguage: Option[String] = None,
  languageOverride: Option[String] = None,
  textVersion: Option[IndexRequest.TextIndex] = None,
  weights: Option[BsonObject] = None,
  bits: Option[Int] = None,
  min: Option[Double] = None,
  max: Option[Double] = None,
  bucketSize: Option[Double] = None,
  sphereVersion: Option[IndexRequest.SphereVersion] = None,
  storageEngine: Option[BsonObject] = None,
  dropDups: Boolean = false
  ) {

  /**
   * Set to true if should create the index in the background
   */
  def withBackground(background: Boolean): IndexRequest = copy(background = background)

  /**
   * Set to true if the index should be unique
   */
  def withUnique(unique: Boolean): IndexRequest = copy(unique = unique)

  /**
   * Sets the name of the index
   */
  def withName(name: String): IndexRequest = copy(name = Some(name))

  /**
   * Set to true if this should index only references documents with the specified field
   */
  def withSparse(sparse: Boolean): IndexRequest = copy(sparse = sparse)

  /**
   * Sets the time to live for documents in the collection
   */
  def expireAfter(expiresAfter: FiniteDuration): IndexRequest = copy(expiresAfter = Some(expiresAfter))

  /**
   * Sets the index version number
   */
  def withVersion(version: Int): IndexRequest = copy(version = Some(version))

  /**
   * Sets the weighting object for use with a text index
   */
  def withWeights(weights: BsonObject): IndexRequest = copy(weights = Some(weights))

  /**
   * Sets the language for the text index
   */
  def withDefaultLanguage(defaultLanguage: String): IndexRequest = copy(defaultLanguage = Some(defaultLanguage))

  /**
   * Sets the name of the field that contains the language string
   */
  def withLanguageOverride(languageOverride: String): IndexRequest = copy(languageOverride = Some(languageOverride))

  /**
   * Set the text index version number
   */
  def withTextVersion(textVersion: IndexRequest.TextIndex): IndexRequest = copy(textVersion = Some(textVersion))

  /**
   * Sets the 2dsphere index version number
   */
  def withSphereVersion(sphereVersion: IndexRequest.SphereVersion): IndexRequest = copy(sphereVersion = Some(sphereVersion))

  /**
   * Sets the number of precision of the stored geohash value of the location data in 2d indexes
   */
  def withBits(bits: Int): IndexRequest = copy(bits = Some(bits))

  /**
   * Sets the lower inclusive boundary for the longitude and latitude values for 2d indexes
   */
  def withMin(min: Double): IndexRequest = copy(min = Some(min))

  /**
   * Sets the upper inclusive boundary for the longitude and latitude values for 2d indexes
   */
  def withMax(max: Double): IndexRequest = copy(max = Some(max))

  /**
   * Sets the specified the number of units within which to group the location values for geoHaystack Indexes
   */
  def withBucketSize(bucketSize: Double): IndexRequest = copy(bucketSize = Some(bucketSize))

  /**
   * Sets the legacy dropDups setting
   */
  def withDropDups(dropDups: Boolean): IndexRequest = copy(dropDups = dropDups)

  /**
   * Sets the storage engine options document for this index
   */
  def withStorageEngine(storageEngineOptions: BsonObject): IndexRequest = copy(storageEngine = Some(storageEngineOptions))
}
object IndexRequest {

  sealed trait SphereVersion
  object SphereVersion {

    case object V1 extends SphereVersion
    case object V2 extends SphereVersion
  }

  sealed trait TextIndex
  object TextIndex {

    case object V1 extends TextIndex
    case object V2 extends TextIndex
  }
}
