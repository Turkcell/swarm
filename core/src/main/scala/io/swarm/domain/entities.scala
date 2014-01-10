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

package io.swarm.domain

import java.util.UUID
import io.swarm.IOTDBException

/**
 * Created by Anil Chalil on 10/22/13.
 */


trait IDEntity {
  def id: UUID
}

trait Disableable {
  def disabled: Boolean
}

trait Client extends IDEntity {

  def activated: Boolean

  def confirmed: Boolean

}

trait UserInfo extends Client with Disableable {
  def username: String

  def name: Option[String]

  def surname: Option[String]

  def email: String

  def credential: String
}

trait ResourceRef extends IDEntity {
  def name: String
}

trait DisableableResourceRef extends ResourceRef

trait DuplicateIDEntity extends IOTDBException

trait IDEntityNotFound extends IOTDBException

object IDEntityNotFound {
  def apply(message: String) = new RuntimeException(message) with DuplicateIDEntity
}

object DuplicateIDEntity {
  def apply(message: String) = new RuntimeException(message) with DuplicateIDEntity

  def apply(t: Throwable) = new RuntimeException(t) with DuplicateIDEntity
}
