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

/**
 * Created by Anil Chalil on 11/1/13.
 */
trait ManagementServiceComponent {
  this: TokenServiceComponent with MetadataRepositoryComponent =>
  val managementService: ManagementService

  trait ManagementService {

    def createOrganization(name: String, adminUsers: Set[AdminUser]): Organization

    def createDatabase(name: String, orgID: UUID): Database

    def getOrganizationInfo(orgID: UUID): Option[OrganizationInfo] = metadataRepository.getOrganizationInfo(orgID)

    def getOrganizationInfoAsync(orgID: UUID): Future[Option[OrganizationInfo]] = future {
      metadataRepository.getOrganizationInfo(orgID)
    }

    def getDatabaseInfo(dbID: UUID): Option[DatabaseInfo] = metadataRepository.getDatabaseInfo(dbID)

    def getDevice(deviceID: UUID): Option[Device] = metadataRepository.getDevice(deviceID)

  }

}

