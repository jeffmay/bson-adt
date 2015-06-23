package adt.bson.test

import adt.bson.mongo.client.TestMongo

/**
 * Run after the tests complete
 */
private class Cleanup {

  println("Shutting down TestMongo clients...")
  TestMongo.shutdown()
}
