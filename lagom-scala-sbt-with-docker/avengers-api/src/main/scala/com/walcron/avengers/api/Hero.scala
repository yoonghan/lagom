package com.walcron.avengers.api

import java.time.Instant
import java.util.UUID
import com.fasterxml.jackson.annotation.JsonIgnore
import play.api.libs.json.Format
import play.api.libs.json.Json

case class Hero @JsonIgnore()(userId: String, message: String, timestamp: Instant, uuid: String) {
  def this(userId: String, message: String) =
    this(userId, message, Hero.defaultTimestamp, Hero.defaultUUID)
}

object Hero {
  implicit object HeroOrder extends Ordering[Hero] {
    override def compare(x: Hero, y: Hero): Int = x.timestamp.compareTo(y.timestamp)
  }

  def apply(userId: String, message: String, timestamp: Option[Instant], uuid: Option[String]): Hero =
    new Hero(userId, message, timestamp.getOrElse(defaultTimestamp), uuid.getOrElse(defaultUUID))

  private def defaultTimestamp = Instant.now()
  private def defaultUUID = UUID.randomUUID().toString()
  
  implicit val format: Format[Hero] = Json.format[Hero]
}