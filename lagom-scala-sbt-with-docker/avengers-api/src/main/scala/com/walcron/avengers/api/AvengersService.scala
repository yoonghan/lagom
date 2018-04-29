package com.walcron.justiceleague.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api._
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import akka.stream.scaladsl.Source
import com.walcron.avengers.api.Hero
import com.lightbend.lagom.scaladsl.api.transport.Method

/**
 * To display persistence into Cassandra and data into Kafka.
 * Topic are registered with topics, but is only triggered depending on *Impl logic
 */
object AvengersService {
  val TOPIC_NAME="Avengers"
}

trait AvengersService extends Service {
  def addHero(id: String): ServiceCall[String, Done]
  def getHeroes(): ServiceCall[NotUsed, String]
  def subHero: ServiceCall[NotUsed, NotUsed]
  
  def heroTopic() : Topic [Hero]

  override final def descriptor = {
    import Service._
    named("avengers-lagom")
      .withCalls(
        restCall(Method.GET, "/api/assembleteam/:name", addHero _),
        restCall(Method.GET, "/api/subTopic", subHero _),
        restCall(Method.GET, "/api/getHeroes", getHeroes _)
      )
      .withTopics(
        topic(AvengersService.TOPIC_NAME, heroTopic)
      )
      .withAutoAcl(true)
  }
}
