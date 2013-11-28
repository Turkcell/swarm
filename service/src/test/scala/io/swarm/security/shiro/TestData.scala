package io.swarm.security.shiro

import io.swarm.domain.{Organization, DatabaseMetadata, Database, AdminUser}
import io.swarm.{Config, UUIDGenerator}
import org.apache.shiro.crypto.hash.Sha1Hash

/**
 * Created by Anil Chalil on 11/21/13.
 */
object TestData {

  val database = Database(UUIDGenerator.secretGenerator.generate(), "testdb", DatabaseMetadata(3600 * 1000 * 24))
  val org = Organization(UUIDGenerator.secretGenerator.generate(), "testorg", Set(database))
  val user = AdminUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", new Sha1Hash("test", Config.userInfoHash).toHex(), true, true, false, Set(org))
}
