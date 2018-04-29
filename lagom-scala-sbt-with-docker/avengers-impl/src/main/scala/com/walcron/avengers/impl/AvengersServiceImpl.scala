package com.walcron.avengers.impl

import com.walcron.justiceleague.api.AvengersService
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import com.lightbend.lagom.scaladsl.api._
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import scala.concurrent.Future
import com.walcron.avengers.api.Hero
import java.time.Instant
import com.datastax.driver.core.Row
import java.util.UUID
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.pubsub.TopicId
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent,AggregateEventTag}
import akka.stream.scaladsl.Flow

/*
 * Describes the service implementation
 */
class AvengersServiceImpl (
    db: CassandraSession,
    avengersService: AvengersService,
    avengersReadSide:AvengersReadSide,
    persistentEntityRegistry: PersistentEntityRegistry
  )(implicit ex: ExecutionContext) extends AvengersService {
  
  private def convertEvent(avengersTimelineEvent: EventStreamElement[AvengersTimelineEvent]): Hero= {
    avengersTimelineEvent.event match {
      case HeroAdded(hero) => println(">>>Publising topic:"+avengersTimelineEvent.entityId)
      new Hero(avengersTimelineEvent.entityId, "")
    }
  }
  
  override def heroTopic(): Topic[Hero] =
    TopicProducer.taggedStreamWithOffset(AvengersTimelineEvent.Tag.allTags.toList) {
      (tag, offset) =>
        persistentEntityRegistry.eventStream(tag, offset)
          .map(ev => (convertEvent(ev), ev.offset))
    }
  
  private def subscribeService():Unit = {
    println(s">>>Subscribed to topic")
    avengersService.heroTopic()
    .subscribe // <-- you get back a Subscriber instance
    .atLeastOnce(
      Flow[Hero].map { msg =>
        println(s">>>Retrieving msg: $msg")
        Done
      }
    )
  }
  
  override def subHero():ServiceCall[NotUsed, NotUsed] = ServiceCall {
    _ => 
      subscribeService()
      Future.successful(akka.NotUsed)
  }
  
  override def addHero(heroId: String):ServiceCall[String, Done] = ServiceCall { heroInfo =>
    val ref = persistentEntityRegistry.refFor[AvengersEntity](heroId)
    ref.ask(UserHeroMessage(heroId))
  }
  
  override def getHeroes(): ServiceCall[NotUsed, String] = 
    ServiceCall { _ => 
      val seqString = avengersReadSide.readHeroes().map(seqHero => seqHero.map(hero => s"Hero: ${hero.userId}").mkString(","))
      seqString
    }
}