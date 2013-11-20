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

package io.swarm.security.shiro

import scala.collection.JavaConverters.seqAsJavaListConverter

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher
import org.apache.shiro.crypto.hash.Sha1Hash
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.subject.PrincipalCollection
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import io.swarm.Config
import io.swarm.UUIDGenerator
import io.swarm.domain.DatabaseUser
trait RealmTestsBase extends BeforeAndAfterEach {
  this: Suite with InMemoryComponents =>
  
  override def beforeEach {
    SecurityUtils.getSubject().logout()
    super.beforeEach
  }
}