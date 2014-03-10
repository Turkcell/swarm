package io.swarm.security.shiro

import io.swarm.UUIDGenerator
import io.swarm.security.HashedAlgorithm
import io.swarm.management.Management._
import io.swarm.management.Management.AdminUserRef
import scala.Some
import io.swarm.management.Management.Organization

/**
 * Created by Anil Chalil on 11/21/13.
 */
object TestData {

  //val database = Database(UUIDGenerator.randomGenerator.generate(), "testdb", DatabaseMetadata(3600 * 1000 * 24),0)
  val domain = DomainRef(UUIDGenerator.randomGenerator.generate(), "test")
  val admin = AdminUserRef(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false)
  val org = Organization(UUIDGenerator.randomGenerator.generate(), "testorg", false, Set(domain), Set(admin))

}
