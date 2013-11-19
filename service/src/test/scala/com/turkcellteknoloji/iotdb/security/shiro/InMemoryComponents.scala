/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.turkcellteknoloji.iotdb.security.shiro

import com.turkcellteknoloji.iotdb.domain.ResourceRepositoryComponent
import com.turkcellteknoloji.iotdb.domain.ClientRepositoryComponent
import com.turkcellteknoloji.iotdb.domain.ClientRepository
import com.turkcellteknoloji.iotdb.domain.DatabaseMetadata
import com.turkcellteknoloji.iotdb.domain.Device
import com.turkcellteknoloji.iotdb.domain.Database
import java.util.UUID
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.UserInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.ResourceRepository
import scala.concurrent._
import scala.collection.mutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import com.turkcellteknoloji.iotdb.domain.Organization
import com.turkcellteknoloji.iotdb.domain.DublicateIDEntity
import com.turkcellteknoloji.iotdb.security.{TokenInfo, TokenRepositoryComponent}

trait InMemoryComponents extends TokenRepositoryComponent with ClientRepositoryComponent with ResourceRepositoryComponent {
  val tokenRepository = new TokenRepository {
    val tokenStore = Map[UUID, TokenInfo]()
    val secretStore = Map[UUID, String]()

    def getTokenInfo(token: OauthBearerToken): TokenInfo = this.synchronized(tokenStore(token.tokenID))

    def getClientSecret(clientID: ClientID): ClientSecret = this.synchronized(ClientSecret(secretStore(clientID.principalID)))

    protected def putTokenInfo(tokenInfo: TokenInfo) = this.synchronized {
      tokenStore += (tokenInfo.uuid -> tokenInfo)
    }

    def saveClientSecret(clientID: ClientID, secret: ClientSecret) = this.synchronized {
      secretStore += (clientID.principalID -> secret.secret)
    }
  }

  val clientRepository = new ClientRepository {
    val adminStore = Map[UUID, UserInfo]()
    val dbUserStore = Map[UUID, UserInfo]()
    val deviceStore = Map[UUID, Device]()

    def getDatabaseUserAsync(uuid: UUID): Future[Option[UserInfo]] = future {
      getDatabaseUser(uuid)
    }

    def getAdminUser(uuid: UUID): Option[UserInfo] = this.synchronized {
      adminStore.get(uuid)
    }

    def getDeviceAsync(uuid: UUID): Future[Option[Device]] = future {
      getDevice(uuid)
    }

    def getAdminUserAsync(uuid: UUID): Future[Option[UserInfo]] = future {
      getAdminUser(uuid)
    }

    def getDatabaseUser(uuid: UUID): Option[UserInfo] = this.synchronized {
      dbUserStore.get(uuid)
    }

    def getDevice(uuid: UUID): Option[Device] = {
      deviceStore.get(uuid)
    }

    def getAdminUserByEmail(email: String): Option[UserInfo] = this.synchronized {
      adminStore.values.find(_.email == email)
    }

    def getAdminUserByUsername(username: String): Option[UserInfo] = this.synchronized(adminStore.values.find(_.username == username))

    def getDatabaseUserByEmail(email: String): Option[UserInfo] = this.synchronized(dbUserStore.values.find(_.email == email))

    def getDatabaseUserByUsername(username: String): Option[UserInfo] = this.synchronized(dbUserStore.values.find(_.username == username))

    def saveAdminUser(user: UserInfo) = this.synchronized {
      if (adminStore.values.forall(_.email != user.email) && adminStore.values.forall(_.username != user.username) && !adminStore.contains(user.id))
        adminStore += (user.id -> user)
      else {
        throw DublicateIDEntity("invalid user")
      }
    }

    def saveDatabaseUser(user: UserInfo) = this.synchronized {
      if (dbUserStore.values.forall(_.email != user.email) && dbUserStore.values.forall(_.username != user.username) && !dbUserStore.contains(user.id))
        dbUserStore += (user.id -> user)
      else {
        throw DublicateIDEntity("invalid user")
      }
    }

    def upsertDatabaseUser(user: UserInfo): Option[UserInfo] = this.synchronized {
      if (dbUserStore.values.forall(u => u.id == user.id || u.email != user.email) && dbUserStore.values.forall(u => u.id == user.id || u.username != user.username))
        dbUserStore.put(user.id, user)
      else {
        throw DublicateIDEntity("invalid user")
      }
    }

    def upsertAdminUser(user: UserInfo): Option[UserInfo] = this.synchronized {
      if (adminStore.values.forall(u => u.id == user.id || u.email != user.email) && adminStore.values.forall(u => u.id == user.id || u.username != user.username))
        adminStore.put(user.id, user)
      else {
        throw DublicateIDEntity("invalid user")
      }
    }

    def upsertDevice(device: Device): Option[Device] = this.synchronized {
      if (deviceStore.values.forall(d => d.deviceID != device.deviceID || d.id == device.id))
        deviceStore.put(device.id, device)
      else
        throw DublicateIDEntity("invalid device")
    }

    def saveDevice(device: Device) = this.synchronized {
      if (deviceStore.values.forall(_.deviceID != device.deviceID) && !deviceStore.contains(device.id))
        deviceStore += (device.id -> device)
      else throw DublicateIDEntity("invalid device")
    }
  }

  val resourceRepository = new ResourceRepository {
    val dbStore = Map[UUID, Database]()
    val orgStore = Map[UUID, Organization]()

    def saveOrganization(org: Organization) = this.synchronized {
      if (orgStore.values.forall(_.name != org.name) && !orgStore.contains(org.id))
        orgStore += (org.id -> org)
      else throw DublicateIDEntity("invalid org")
    }

    def getDatabase(id: UUID): Option[Database] = this.synchronized(dbStore.get(id))

    def getDatabaseInfo(id: UUID): Option[DatabaseInfo] = getDatabase(id).map(db => DatabaseInfo(db.id, db.name))

    def getDatabaseInfoAsync(uuid: UUID): Future[Option[DatabaseInfo]] = future {
      getDatabaseInfo(uuid)
    }

    def getDatabaseMetadata(id: UUID): Option[DatabaseMetadata] = getDatabase(id).map(db => db.metadata)

    def getOrganization(id: UUID): Option[Organization] = this.synchronized(orgStore.get(id))

    def getOrganizationInfo(id: UUID): Option[OrganizationInfo] = getOrganization(id).map(org => OrganizationInfo(org.id, org.name))

    def getOrganizationInfoAsync(uuid: UUID): Future[Option[OrganizationInfo]] = future {
      getOrganizationInfo(uuid)
    }

    def upsertOrganization(org: Organization): Option[Organization] = this.synchronized {
      if (orgStore.values.forall(o => o.name != org.name || o.id == org.id))
        orgStore.put(org.id, org)
      else
        throw DublicateIDEntity("invalid org")
    }

    def saveDatabase(db: Database): Unit = this.synchronized {
      if (dbStore.values.forall(_.name != db.name) && !dbStore.contains(db.id))
        dbStore += (db.id -> db)
      else throw DublicateIDEntity("invalid database")
    }

    def upsertDatabase(db: Database): Option[Database] = this.synchronized {
      if (dbStore.values.forall(d => d.name != db.name || d.id == db.id))
        dbStore.put(db.id, db)
      else throw DublicateIDEntity("invalid database")
    }

  }
}