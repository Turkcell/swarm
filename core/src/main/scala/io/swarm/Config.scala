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

package io.swarm

import com.typesafe.config.{ConfigException, ConfigFactory}
import org.apache.shiro.util.ByteSource

/**
 * Created by Anil Chalil on 11/11/13.
 */
object Config {

  protected def getOptionalBoolean(path: String): Option[Boolean] = try {
    Some(conf.getBoolean(path))
  } catch {
    case e: ConfigException.Missing =>
      None
  }

  protected def getOptionalLong(path: String): Option[Long] = try {
    Some(conf.getLong(path))
  } catch {
    case e: ConfigException.Missing =>
      None
  }

  val conf = ConfigFactory.load
  val clientTokenSecretSalt = conf.getString("security.oauth.clientSecretSalt")
  val tokenSecretSalt = conf.getString("security.oauth.tokenSecretSalt")
  val userInfoHash = ByteSource.Util.bytes(conf.getString("security.users.credentialSalt"))
  val enableSandBoxDB = getOptionalBoolean("provisioning.organization.enableSandBoxDB").getOrElse(false)
  val defaultDBOauthTTL = getOptionalLong("provisioning.database.defaultDBOauthTTL").getOrElse(0L)

  val adminUsersRequireActivation = getOptionalBoolean("provisioning.organization.adminUserActivation").getOrElse(false)

  val adminUsersRequireConfirmation = getOptionalBoolean("provisioning.organization.adminUserConfirmation").getOrElse(false)

}
