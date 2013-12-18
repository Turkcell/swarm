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

import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalacheck.Gen

/**
 * Created by Anil Chalil on 11/19/13.
 */
class CustomPermissionTests extends FlatSpec with ShouldMatchers {
  def combinationsOf(value: String) = Gen.oneOf((for (i <- (1 to value.split(",").length)) yield value.split(",").combinations(i).toList).flatten)

  val tagsGen = Gen.containerOf[Array, String](Gen.alphaStr)

  val orgAllPermission = new CustomPermission("organizations:*:orgid")
  val orgStarPermission = new CustomPermission("organizations:*:*")
  val orgExpandedPermission = new CustomPermission("organizations:admin,access,get,put,post,delete:*")
  val orgPutPermission = new CustomPermission("organizations:put:orgid")

  val dbAllPermission = new CustomPermission("databases:*:dbid")
  val dbAllPermissionToTemp = new CustomPermission("databases:*:dbid:temp")
  val dbPostPermissionToTemp = new CustomPermission("databases:post:dbid:temp")
  val dbPostPermissionToTempHumidity = new CustomPermission("databases:post:dbid:temp,humidity")
  val allDbPostPermissionToTemp = new CustomPermission("databases:post:*:temp")

  "empty org permission" should "implied by" in {
    orgStarPermission.implies(new CustomPermission("organizations::")) should be(true)
  }

  "dbPostPermissionToTemp " should "imply" in {
    dbPostPermissionToTemp.implies(new CustomPermission("databases:post:dbid:temp,temp2")) should be(true)
    dbPostPermissionToTemp.implies(new CustomPermission("databases:post:dbid:temp2")) should be(false)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:post:dbid:temp2")) should be(false)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:post:dbid:temp")) should be(false)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:post:dbid:humidity")) should be(false)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:post:dbid:humidity,temp")) should be(true)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:post:dbid:humidity,temp,temp2")) should be(true)
    dbPostPermissionToTempHumidity.implies(new CustomPermission("databases:get:dbid:humidity,temp,temp2")) should be(false)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), combinationsOf("orgid,orgid2,orgid3,orgid4,orgid5")) {
    (p: Array[String], orgsID: Array[String]) =>
      orgStarPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:${orgsID.mkString(",")}")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), combinationsOf("orgid,orgid2,orgid3,orgid4,orgid5")) {
    (p: Array[String], orgsID: Array[String]) =>
      whenever(!orgsID.isEmpty && !orgsID.forall(_ == "orgid")) {
        orgStarPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:${orgsID.mkString(",")}")) should be(true)
      }
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), combinationsOf("orgid,orgid2,orgid3,orgid4,orgid5")) {
    (p: Array[String], orgsID: Array[String]) =>
      orgExpandedPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:${orgsID.mkString(",")}")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), Gen.alphaStr) {
    (p: Array[String], orgID: String) =>
      orgStarPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:$orgID")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete")) {
    p =>
      orgAllPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:orgid")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete")) {
    p =>
      whenever(!p.contains("put")) {
        orgPutPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:orgid")) should be(false)
      }
  }

  forAll(combinationsOf("admin,access,get,put,post,delete")) {
    p =>
      dbAllPermission.implies(new CustomPermission(s"databases:${p.mkString(",")}:dbid")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), Gen.alphaStr) {
    (p: Array[String], tag: String) =>
      dbAllPermission.implies(new CustomPermission(s"databases:${p.mkString(",")}:dbid:$tag")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), tagsGen) {
    (p: Array[String], tags: Array[String]) =>
      dbAllPermission.implies(new CustomPermission(s"databases:${p.mkString(",")}:dbid:${tags.mkString(",")}")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete")) {
    p =>
      dbAllPermissionToTemp.implies(new CustomPermission(s"databases:${p.mkString(",")}:dbid:temp")) should be(true)
  }

  forAll(combinationsOf("admin,access,get,put,post,delete"), tagsGen) {
    (p: Array[String], tags: Array[String]) =>
      whenever(!tags.isEmpty && !p.isEmpty && Permissions.isValidRights(p)) {
        dbAllPermissionToTemp.implies(new CustomPermission(s"databases:${p.mkString(",")}:dbid:temp,${tags.mkString(",")}")) should be(true)
      }
  }
}
