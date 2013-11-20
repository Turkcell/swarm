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

import org.apache.shiro.authz.permission.WildcardPermission
import org.apache.shiro.authz.Permission
import scala.collection.JavaConverters._

/**
 * Created by Anil Chalil on 11/19/13.
 */
class CustomPermission(permission: String) extends WildcardPermission(permission, false) {
  override def implies(p: Permission): Boolean = {
    // By default only supports comparisons with other
    // PathBasedWildcardPermission
    if (!p.isInstanceOf[CustomPermission]) {
      false
    } else {
      val otherParts = p.asInstanceOf[CustomPermission].getParts.asScala
      val isDB = if (otherParts.isEmpty) false else otherParts(0).contains("databases")
      otherParts.zipWithIndex.collectFirst {
        case (otherPart, i) if i > (getParts.size - 1) => true
        case (otherPart, i) if isDB && i > 2 => otherPart.containsAll(getParts.get(i))
        case (otherPart, i) if !getParts.get(i).contains(WildcardPermission.WILDCARD_TOKEN) && !getParts.get(i).containsAll(otherPart) => false
      } getOrElse {
        getParts.asScala.takeRight(getParts.size - otherParts.size).zipWithIndex.forall {
          case (part, i) =>
            if (isDB && (i + otherParts.size) > 2) {
              part.isEmpty
            } else {
              part.contains(WildcardPermission.WILDCARD_TOKEN)
            }
        }
      }
    }
  }
}
