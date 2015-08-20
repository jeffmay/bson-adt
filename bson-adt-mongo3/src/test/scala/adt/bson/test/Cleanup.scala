package adt.bson.test

import adt.bson.mongo.client.TestMongo

/**
 * Initialize this class in sbt after the tests run to cleanup all mongo databases:
 *
 * {{{
 *   testOptions in Test += Tests.Cleanup {
 *     (loader: java.lang.ClassLoader) =>
 *       println("Running test cleanup for bson-adt-mongo-async project...")
 *       loader.loadClass("adt.bson.test.Cleanup").newInstance
 *   }
 * }}}
 */
private class Cleanup {

  println("Shutting down TestMongo clients...")
  TestMongo.shutdown()
}
