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

import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalacheck.Gen

/**
 * Created by Anil Chalil on 11/19/13.
 */
class CustomPermissionTests extends FlatSpec with ShouldMatchers {
  def combinationsOf(value: String) = Gen.oneOf((for (i <- (1 to value.split(",").length)) yield value.split(",").combinations(i).toList).flatten)
  val orgAllPermission = new CustomPermission("organizations:*:orgid")
  forAll(combinationsOf("admin,access,get,put,post,delete")) {
    p =>
      orgAllPermission.implies(new CustomPermission(s"organizations:${p.mkString(",")}:orgid")) should be(true)
  }
}
