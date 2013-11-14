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


package com.turkcellteknoloji.iotdb.security

import com.turkcellteknoloji.iotdb.IOTDBException

/**
 * Created by Anil Chalil
 */
trait TokenException extends IOTDBException

trait NotTokenException extends TokenException

trait BadTokenException extends TokenException

trait BadClientIDException extends TokenException

trait NotClientIDException extends TokenException

trait NotClientSecretException extends TokenException

trait InvalidClientIDSecretTokenException extends TokenException

trait ExpiredTokenException extends TokenException {
  val delta: Long
}

object ExpiredTokenException {
  def apply(value: Long) = new RuntimeException(s"token expired $value millisecond ago!") with ExpiredTokenException {
    val delta = value
  }
}

object NotClientSecretException {
  def apply(e: Throwable) = new RuntimeException(e) with NotClientSecretException

  def apply(m: String) = new RuntimeException(m) with NotClientSecretException
}

object InvalidClientIDSecretTokenException {
  def apply(m: String) = new RuntimeException(m) with InvalidClientIDSecretTokenException
}

object NotClientIDException {
  def apply(e: Throwable) = new RuntimeException(e) with NotClientIDException

  def apply(m: String) = new RuntimeException(m) with NotClientIDException
}

object BadClientIDException {
  def apply(e: Throwable) = new RuntimeException(e) with BadClientIDException
}

trait BadClientSecretException extends TokenException

object BadTokenException {
  def apply(message: String) = new RuntimeException(message) with BadTokenException
}

object NotTokenException {
  def apply(message: String) = new RuntimeException(message) with NotTokenException

  def apply(throwable: Throwable) = new RuntimeException(throwable) with NotTokenException
}
