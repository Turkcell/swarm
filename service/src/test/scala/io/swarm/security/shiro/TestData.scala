package io.swarm.security.shiro

import io.swarm.domain.{Organization, DatabaseMetadata, Database, AdminUser}
import io.swarm.UUIDGenerator
import io.swarm.security.HashedAlgorithm

/**
 * Created by Anil Chalil on 11/21/13.
 */
object TestData {

  val database = Database(UUIDGenerator.randomGenerator.generate(), "testdb", DatabaseMetadata(3600 * 1000 * 24))
  val org = Organization(UUIDGenerator.randomGenerator.generate(), "testorg", Set(database))
  val user = AdminUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set(org))
}
