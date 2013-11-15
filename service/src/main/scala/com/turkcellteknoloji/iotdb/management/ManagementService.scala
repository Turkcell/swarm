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

package com.turkcellteknoloji.iotdb.management

import com.turkcellteknoloji.iotdb.domain._
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.Device
import java.util.UUID
import com.turkcellteknoloji.iotdb.security.tokens.TokenServiceComponent
import scala.concurrent._
import com.turkcellteknoloji.iotdb.security.{AuthPrincipalType, ClientID, OauthBearerToken}

/**
 * Created by Anil Chalil on 11/1/13.
 */
trait ManagementServiceComponent {
  this: TokenServiceComponent with ResourceRepositoryComponent =>
  val managementService: ManagementService

  trait ManagementService {
    def getUserInfoAsync(token: OauthBearerToken): Future[Option[UserInfo]]

    def getUserInfo(token: OauthBearerToken): Option[UserInfo]

    def getUserInfoAsync(principal: String): Future[Option[UserInfo]]

    def getEntityInfoAsync(token: OauthBearerToken): Future[Option[ResourceRef]]

    def getEntityInfoAsync(clientID: ClientID): Future[Option[ResourceRef]]

    def createOrganization(name: String, adminUsers: Set[AdminUser]): Organization

    def createDatabase(name: String, orgID: UUID): Database

    def getOrganizationInfo(orgID: UUID): Option[OrganizationInfo] = resourceRepository.getOrganizationInfo(orgID)

    def getOrganizationInfoAsync(orgID: UUID): Future[Option[OrganizationInfo]] = future {
      resourceRepository.getOrganizationInfo(orgID)
    }

    def getDatabaseInfo(dbID: UUID): Option[DatabaseInfo] = resourceRepository.getDatabaseInfo(dbID)

    def getDatabaseInfoAsync(uuid: UUID) = future {
      getDatabaseInfo(uuid)
    }

    def getDevice(deviceID: UUID): Option[Device] = resourceRepository.getDevice(deviceID)

  }

}

