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
import java.util.UUID
import io.swarm.security.TokenRepositoryComponent
import io.swarm.management.Management.{OrganizationRef, DomainRef, Domain}

/**
 * Created by Anil Chalil on 11/1/13.
 */
trait ManagementServiceComponent {
  this: TokenRepositoryComponent =>
  val managementService: ManagementService

  trait ManagementService {
    private def isValidEmail(email: String): Boolean =
      """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

    def createSandBoxDomain(): Domain

    def createOrganizationWithAdmin(organizationName: String, name: Option[String], surname: Option[String], username: String, email: String, password: String): (OrganizationRef, UserInfo)

    def createDomain(name: String, orgID: UUID): Domain


    def getDomainInfo(dbID: UUID): Option[DomainRef]

    def getDomainInfoAsync(uuid: UUID)

  }

}

