package com.turkcellteknoloji.iotdb.security

import java.util.UUID
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType

/**
 * Created by capacman on 11/1/13.
 */
case class AuthPrincipalInfo(val `type`: AuthPrincipalType, val uuid: UUID)
