package adt.bson

// TODO: Add path information to this error message
/**
 * Exception that is thrown when a reader cannot parse a BsonValue in the manner expected.
 * @param errorCode The key for this type of error
 * @param bson The bson that was being parsed
 * @param message The detailed message with enough information to debug. This is what is printed in the stack trace.
 */
class UnexpectedBsonException(errorCode: String, bson: BsonValue, message: String = null)
  extends Exception(Option(message) getOrElse s"Error ($errorCode) while reading BSON: ${Bson.prettyPrint(bson)}")
