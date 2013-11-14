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

package com.turkcellteknoloji.iotdb.domain

import java.util.UUID

/**
 * Created by Anil Chalil on 10/22/13.
 */
case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String])

trait UserInfo {
  def id: UUID

  def username: String

  def name: String

  def surname: String

  def email: String

  def activated: Boolean

  def confirmed: Boolean

  def disabled: Boolean
}

case class AdminUser(id: UUID, name: String, surname: String, username: String, email: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

case class DatabaseUser(id: UUID, name: String, surname: String, username: String, email: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

trait EntityRef {
  def id: UUID

  def name: String
}

trait OrganizationRef extends EntityRef

trait DatabaseRef extends EntityRef

trait DeviceRef extends EntityRef

case class OrganizationInfo(id: UUID, name: String) extends OrganizationRef

case class Organization(id: UUID, name: String, users: Set[AdminUser]) extends OrganizationRef

case class DatabaseInfo(id: UUID, name: String) extends DatabaseRef

case class Database(id: UUID, name: String, owner: Organization) extends DatabaseRef

case class Device(id: UUID, deviceID: String) extends DeviceRef {
  def name = deviceID
}


trait MetadataRepository {
  def createOrganization(name: String, users: Set[AdminUser]): Organization

  def updateOrganization(org: Organization)
}

