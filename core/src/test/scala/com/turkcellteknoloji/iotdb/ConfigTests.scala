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

package com.turkcellteknoloji.iotdb

import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.apache.shiro.util.ByteSource
import com.typesafe.config.ConfigFactory

/**
 * Created by Anil Chalil on 11/12/13.
 */
@RunWith(classOf[JUnitRunner])
class ConfigTests extends FlatSpec with ShouldMatchers {

  "config" should "print configuration" in {
    println(ConfigFactory.load())
  }
  
  it should "provide tokensecret " in {
    Config.tokenSecretSalt === "mytokenSecret"
  }

  it should "provide client secret" in {
    Config.clientTokenSecretSalt === "myclientSecret"
  }

  it should "provide user slat value" in {
    assert(Config.userInfoHash.getBytes() sameElements ByteSource.Util.bytes("mycredentialssalt").getBytes)
  }
}
