package com.turkcellteknoloji.iotdb.domain

import java.util.UUID

/**
 * Created by Anil Chalil on 10/22/13.
 */
case class Series(val id: UUID, val key: String, val name: Option[String], val tags: Set[String], val attributes: Map[String, String])

case class User(val id: UUID, val name: String, val surname: String, val username: String, val email: String)

case class Database(val id: UUID, val name: String, val owner: User)