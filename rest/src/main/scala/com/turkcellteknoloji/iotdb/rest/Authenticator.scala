package com.turkcellteknoloji.iotdb.rest

/**
 * Created by capacman on 11/11/13.
 */
trait OrganizationAuthenticator {
  protected def organizationOnly(action: => Any) = {
    authenticate(action)
  }

  private def authenticate(action: => Any) = {

  }
}
