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

package io.swarm.security.shiro

import org.apache.shiro.authc.pam.{UnsupportedTokenException, ModularRealmAuthenticator}
import org.apache.shiro.authc.AuthenticationToken
import scala.collection.JavaConverters._

/**
 * Created by Anil Chalil on 11/14/13.
 */
class ExclusiveRealmAuthenticator extends ModularRealmAuthenticator {

  override def doAuthenticate(authenticationToken: AuthenticationToken) = {
    assertRealmsConfigured
    val realm = getRealms.asScala.find(_.supports(authenticationToken))
    realm match {
      case Some(r) => doSingleRealmAuthentication(r, authenticationToken)
      case None => throw new UnsupportedTokenException(s"unsupported token ${authenticationToken.getClass.getCanonicalName}")
    }
  }
}
