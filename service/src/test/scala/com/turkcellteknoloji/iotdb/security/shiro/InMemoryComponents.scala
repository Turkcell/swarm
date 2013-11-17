package com.turkcellteknoloji.iotdb.security.shiro

import com.turkcellteknoloji.iotdb.security.TokenRepositoryComponent
import com.turkcellteknoloji.iotdb.domain.ResourceRepositoryComponent
import com.turkcellteknoloji.iotdb.domain.ClientRepositoryComponent
import com.turkcellteknoloji.iotdb.domain.ClientRepository
import com.turkcellteknoloji.iotdb.domain.DatabaseMetadata
import com.turkcellteknoloji.iotdb.domain.Device
import com.turkcellteknoloji.iotdb.domain.Database
import java.util.UUID
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.security.ClientSecret
import com.turkcellteknoloji.iotdb.security.ClientID
import com.turkcellteknoloji.iotdb.domain.UserInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.Organization
import com.turkcellteknoloji.iotdb.security.TokenInfo
import com.turkcellteknoloji.iotdb.domain.ResourceRepository
import com.turkcellteknoloji.iotdb.security.OauthBearerToken
import scala.concurrent._
import scala.collection.mutable.Map
import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext.Implicits.global

trait InMemoryComponents extends TokenRepositoryComponent with ClientRepositoryComponent with ResourceRepositoryComponent {
  val tokenRepository = new TokenRepository {
    val tokenStore = (new java.util.concurrent.ConcurrentHashMap[UUID, TokenInfo]).asScala
    val secretStore = (new java.util.concurrent.ConcurrentHashMap[String, String]).asScala

    def getTokenInfo(token: OauthBearerToken): TokenInfo = tokenStore(token.tokenID)

    def getClientSecret(clientID: ClientID): ClientSecret = ClientSecret(secretStore(clientID.id))
    protected def putTokenInfo(tokenInfo: TokenInfo) {
      tokenStore += (tokenInfo.uuid -> tokenInfo)
    }
  }

  val clientRepository = new ClientRepository {
    val adminStore = Map[UUID, UserInfo]()
    val dbUserStore = Map[UUID, UserInfo]()
    val deviceStore = Map[UUID, Device]()
    def getDatabaseUserAsync(uuid: UUID): Future[Option[UserInfo]] = future { getDatabaseUser(uuid) }

    def getAdminUser(uuid: UUID): Option[UserInfo] = this.synchronized { adminStore.get(uuid) }

    def getDeviceAsync(uuid: UUID): Future[Option[Device]] = future { getDevice(uuid) }

    def getAdminUserAsync(uuid: UUID): Future[Option[UserInfo]] = future { getAdminUser(uuid) }

    def getDatabaseUser(uuid: UUID): Option[UserInfo] = this.synchronized { dbUserStore.get(uuid) }

    def getDevice(uuid: UUID): Option[Device] = { deviceStore.get(uuid) }

    def getAdminUserByEmail(email: String): Option[UserInfo] = this.synchronized { adminStore.values.find(_.email == email) }

    def getAdminUserByUsername(username: String): Option[UserInfo] = this.synchronized(adminStore.values.find(_.username == username))

    def getDatabaseUserByEmail(email: String): Option[UserInfo] = this.synchronized(dbUserStore.values.find(_.email == email))

    def getDatabaseUserByUsername(username: String): Option[UserInfo] = this.synchronized(dbUserStore.values.find(_.username == username))

    def saveAdminUser(user: UserInfo) = this.synchronized {
      if (adminStore.values.forall(_.email != user.email) && adminStore.values.forall(_.username != user.username) && !adminStore.contains(user.id))
        adminStore += (user.id -> user)
      else {
        //TODO exception should change
        throw new IllegalArgumentException("invalid user")
      }
    }

    def saveDatabaseUser(user: UserInfo) = this.synchronized {
      if (dbUserStore.values.forall(_.email != user.email) && dbUserStore.values.forall(_.username != user.username) && !dbUserStore.contains(user.id))
        dbUserStore += (user.id -> user)
      else {
        //TODO exception should change
        throw new IllegalArgumentException("invalid user")
      }
    }

    def upsertDatabaseUser(user: UserInfo): Option[UserInfo] = this.synchronized {
      if (dbUserStore.values.forall(u => u.id == user.id || u.email != user.email) && dbUserStore.values.forall(u => u.id == user.id || u.username != user.username))
        dbUserStore.put(user.id, user)
      else {
        //TODO exception should change
        throw new IllegalArgumentException("invalid user")
      }
    }

    def upsertAdminUser(user: UserInfo): Option[UserInfo] = this.synchronized {
      if (adminStore.values.forall(u => u.id == user.id || u.email != user.email) && adminStore.values.forall(u => u.id == user.id || u.username != user.username))
        adminStore.put(user.id, user)
      else {
        //TODO exception should change
        throw new IllegalArgumentException("invalid user")
      }
    }
  }

  val resourceRepository = new ResourceRepository {
    val dbStore = (new java.util.concurrent.ConcurrentHashMap[UUID, Database]).asScala
    val orgStore = (new java.util.concurrent.ConcurrentHashMap[UUID, Organization]).asScala

    def saveOrganization(org: Organization): Option[Organization] = orgStore.putIfAbsent(org.id, org)
    def getDatabase(id: UUID): Option[Database] = {
      val db = dbStore(id)
      if (db == null) None else Some(db)
    }
    def getDatabaseInfo(id: UUID): Option[DatabaseInfo] = getDatabase(id).map(db => DatabaseInfo(db.id, db.name))
    def getDatabaseInfoAsync(uuid: UUID): Future[Option[DatabaseInfo]] = future { getDatabaseInfo(uuid) }
    def getDatabaseMetadata(id: UUID): Option[DatabaseMetadata] = getDatabase(id).map(db => db.metadata)
    def getOrganization(id: UUID): Option[Organization] = {
      val org = orgStore(id)
      if (org == null) None else Some(org)
    }
    def getOrganizationInfo(id: UUID): Option[OrganizationInfo] = getOrganization(id).map(org => OrganizationInfo(org.id, org.name))
    def getOrganizationInfoAsync(uuid: UUID): Future[Option[OrganizationInfo]] = future { getOrganizationInfo(uuid) }
    def updateOrganization(org: Organization): Option[Organization] = orgStore replace (org.id, org)
  }
}