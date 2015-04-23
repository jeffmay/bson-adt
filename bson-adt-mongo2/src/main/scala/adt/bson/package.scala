package adt

import scala.language.higherKinds

package object bson {

  @deprecated("Use adt.bson.mongodb.DBCompanion instead.", "1.3.0")
  type DBCompanion[-A, T] = mongodb.DBCompanion[A, T]

  @deprecated("Use adt.bson.mongodb.DBExtractor instead.", "1.3.0")
  type DBExtractor[T] = mongodb.DBExtractor[T]

  @deprecated("Use adt.bson.mongodb.DBValue instead.", "1.3.0")
  type DBValue = mongodb.DBValue

  @deprecated("Use adt.bson.mongodb.DBValueCompanion instead.", "1.3.0")
  type DBValueCompanion = mongodb.DBValueCompanion

  @deprecated("Use adt.bson.mongodb.DBValueExtractors instead.", "1.3.0")
  type DBValueExtractors = mongodb.DBValueExtractors

  @deprecated("Use adt.bson.mongodb.DefaultDBValueExtractor instead.", "1.3.0")
  type DefaultDBValueExtractor = mongodb.DefaultDBValueExtractor
}
