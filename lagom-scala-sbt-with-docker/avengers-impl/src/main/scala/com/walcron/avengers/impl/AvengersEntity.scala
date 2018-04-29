package com.walcron.avengers.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import play.api.libs.json.Format
import play.api.libs.json.Json
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import java.time.LocalDateTime
import com.walcron.avengers.api.Hero
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}
import scala.collection.immutable.Seq
import com.walcron.avengers.api.Hero
import scala.concurrent.ExecutionContext
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards

/**
 * This program only runs on Shards event streams.
 */
class AvengersEntity(db: CassandraSession) extends PersistentEntity {

  override type Command = AvengersCommand[_]
  override type Event = AvengersTimelineEvent
  override type State = AvengersState

  override def initialState: AvengersState = AvengersState(new Hero("",""), LocalDateTime.now.toString)

  override def behavior: Behavior = {
    case AvengersState(message, _) => Actions()
    .onCommand[UserHeroMessage, Done] {
      case (UserHeroMessage(heroId), ctx, state) =>
        println(">>>Adding Hero"+heroId)
        val event = HeroAdded(new Hero(heroId, ""))
        ctx.thenPersist(event) { _ =>
          ctx.reply(Done)
        }
    }
    .onEvent {
      case (HeroAdded(hero), state) =>
        println(">>>Creating Hero State"+hero.userId)
        AvengersState(hero, LocalDateTime.now().toString)
    }
  }
}

sealed trait AvengersCommand[R] extends ReplyType[R]

case class UserHeroMessage(message: String) extends AvengersCommand[Done]

sealed trait AvengersTimelineEvent extends AggregateEvent[AvengersTimelineEvent] {
  override def aggregateTag: AggregateEventShards[AvengersTimelineEvent] = AvengersTimelineEvent.Tag
}

object AvengersTimelineEvent {
  val NumShards = 3
  val Tag = AggregateEventTag.sharded[AvengersTimelineEvent](NumShards)
}

case class HeroAdded(hero: Hero) extends AvengersTimelineEvent

object HeroAdded {
  implicit val format: Format[HeroAdded] = Json.format
}

case class AvengersState(hero: Hero, timestamp: String)

object AvengersState {
  implicit val format: Format[AvengersState] = Json.format
}

object AvengersRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Hero],
    JsonSerializer[HeroAdded],
    JsonSerializer[AvengersState]
  )
}
