package com.turkcellteknoloji.iotdb.security

import com.turkcellteknoloji.iotdb.IOTDBException

/**
 * Created with IntelliJ IDEA.
 * User: capacman
 * Date: 11/13/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
trait TokenException extends IOTDBException

trait NotTokenException extends TokenException

trait BadTokenException extends TokenException

trait BadClientIDException extends TokenException

trait NotClientIDException extends TokenException

trait NotClientSecretException extends TokenException

object NotClientSecretException {
  def apply(e: Throwable) = new RuntimeException(e) with NotClientSecretException
}

object NotClientIDException {
  def apply(e: Throwable) = new RuntimeException(e) with NotClientIDException
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
