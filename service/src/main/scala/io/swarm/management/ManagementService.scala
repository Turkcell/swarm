/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.swarm.management

import io.swarm.domain._
import io.swarm.domain.OrganizationInfo
import io.swarm.domain.DatabaseInfo
import java.util.UUID
import scala.concurrent._
import ExecutionContext.Implicits.global
import io.swarm.security.{AnonymousAuthorizer, TokenRepositoryComponent}
import io.swarm.{Config, UUIDGenerator}

/**
 * Created by Anil Chalil on 11/1/13.
 */
trait ManagementServiceComponent {
  this: TokenRepositoryComponent with ResourceRepositoryComponent with ClientRepositoryComponent =>
  val managementService: ManagementService

  trait ManagementService extends AnonymousAuthorizer {
    private def isValidEmail(email: String): Boolean =
      """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

    def createSandBoxDB(): Database = {
      Database(UUIDGenerator.randomGenerator.generate(), "sandbox", DatabaseMetadata(Config.defaultDBOauthTTL))
    }

    def createOrganizationWithAdmin(organizationName: String, name: Option[String], surname: Option[String], username: String, email: String): Organization = isAnonymous {
      require(username != null && !username.isEmpty, "username could not be empty or null")
      require(email != null && isValidEmail(email), "email should be correct!")
      if (Config.enableSandBoxDB)
        resourceRepository.saveOrganization(Organization(UUIDGenerator.randomGenerator.generate(), organizationName, Set(createSandBoxDB())))
      else
        resourceRepository.saveOrganization(Organization(UUIDGenerator.randomGenerator.generate(), organizationName, Set()))
    }

    def createDatabase(name: String, orgID: UUID): Database

    def getOrganizationInfo(orgID: UUID): Option[OrganizationInfo] = resourceRepository.getOrganizationInfo(orgID)

    def getOrganizationInfoAsync(orgID: UUID): Future[Option[OrganizationInfo]] = future {
      resourceRepository.getOrganizationInfo(orgID)
    }

    def getDatabaseInfo(dbID: UUID): Option[DatabaseInfo] = resourceRepository.getDatabaseInfo(dbID)

    def getDatabaseInfoAsync(uuid: UUID) = future {
      getDatabaseInfo(uuid)
    }

  }

}

