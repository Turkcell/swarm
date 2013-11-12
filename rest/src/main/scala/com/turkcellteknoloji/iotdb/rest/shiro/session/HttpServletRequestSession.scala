package com.turkcellteknoloji.iotdb.server.shiro.session

import org.apache.shiro.session.Session
import java.util
import java.util.{UUID, Date}
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Created by capacman on 10/31/13.
 */
class HttpServletRequestSession(_request: HttpServletRequest, _host: String) extends Session {
  private val id = UUID.randomUUID()
  private val host = _host
  private val request = _request
  private val start = new Date()

  def setAttribute(key: scala.Any, value: scala.Any) {
    request.setAttribute(stringify(key), value)
  }

  def stop() {}

  def touch() {}

  def setTimeout(maxIdleTimeInMillis: Long) {}

  def getId: Serializable = id

  def getStartTimestamp: Date = start

  def getLastAccessTime: Date = start

  def getTimeout: Long = -1

  def getHost: String = host

  def getAttributeKeys: util.Collection[Object] = java.util.Collections.list(request.getAttributeNames).asInstanceOf[util.Collection[Object]]

  private def stringify(key: Any) = if (key == null) null else key.toString

  def getAttribute(key: scala.Any): AnyRef = request.getAttribute(stringify(key))

  def removeAttribute(key: scala.Any): AnyRef = {
    val s: String = stringify(key)
    val formerValue = request.getAttribute(s)
    request.removeAttribute(s)
    formerValue
  }
}
