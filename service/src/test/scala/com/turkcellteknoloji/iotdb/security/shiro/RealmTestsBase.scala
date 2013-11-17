package com.turkcellteknoloji.iotdb.security.shiro

import scala.collection.JavaConverters.seqAsJavaListConverter

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher
import org.apache.shiro.crypto.hash.Sha1Hash
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.subject.PrincipalCollection
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import com.turkcellteknoloji.iotdb.Config
import com.turkcellteknoloji.iotdb.UUIDGenerator
import com.turkcellteknoloji.iotdb.domain.DatabaseUser
trait RealmTestsBase extends BeforeAndAfterEach {
  this: Suite with InMemoryComponents =>
  
  override def beforeEach {
    SecurityUtils.getSubject().logout()
    super.beforeEach
  }
}