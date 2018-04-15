package com.walcron.justiceleague.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}


object JusticeLeagueService {
  val TOPIC_NAME="Justice League Assemble"
}

trait JusticeLeagueService extends Service {
  def callHero(id: String): ServiceCall[NotUsed, String]
  
  override final def descriptor = {
    import Service._
    named("justice-lagom")
      .withCalls(
        pathCall("/api/hero/:name", callHero _)
      )
      .withAutoAcl(true)
  }
}