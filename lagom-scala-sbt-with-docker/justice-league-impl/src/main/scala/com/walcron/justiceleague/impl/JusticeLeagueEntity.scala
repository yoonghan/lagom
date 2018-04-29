package com.walcron.justiceleague.impl

import java.time.LocalDateTime

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}
import scala.collection.immutable.Seq

sealed trait HeroCommand[R] extends ReplyType[R]

case class Hero(name: String) extends HeroCommand[String]

object Hero {
  implicit val format: Format[Hero] = Json.format
}

class JusticeLeagueEntity extends PersistentEntity {
  
  override type Command = HeroCommand[_]
  override type State = HeroState

  override def initialState: HeroState = HeroState("GalGadot", LocalDateTime.now.toString)

  override def behavior: Behavior = {
    case HeroState(message, _) => Actions().onReadOnlyCommand[Hero, String] {
      case (Hero(name), ctx, state) =>
        ctx.reply(s"$message, $name!")

    }
  }
}

sealed trait HeroEvent extends AggregateEvent[HeroEvent] {
  def aggregateTag = HeroEvent.Tag
}

object HeroEvent {
  val Tag = AggregateEventTag[HeroEvent]
}

case class HeroState(message: String, timestamp: String)

object HeroState {
  implicit val format: Format[HeroState] = Json.format
}

object JusticeLeagueSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Hero],
    JsonSerializer[HeroState]
  )
}