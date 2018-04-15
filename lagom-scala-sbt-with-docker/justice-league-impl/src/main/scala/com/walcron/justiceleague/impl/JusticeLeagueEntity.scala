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
  override type Event = HeroEvent
  override type State = HeroState

  override def initialState: HeroState = HeroState("GalGadot", LocalDateTime.now.toString)

  override def behavior: Behavior = {
    case HeroState(message, _) => Actions().onCommand[UseGreetingMessage, Done] {

      case (UseGreetingMessage(newMessage), ctx, state) =>
        ctx.thenPersist(
          GreetingMessageChanged(newMessage)
        ) { _ =>
          ctx.reply(Done)
        }

    }.onReadOnlyCommand[Hero, String] {
      case (Hero(name), ctx, state) =>
        ctx.reply(s"$message, $name!")

    }.onEvent {
      case (GreetingMessageChanged(newMessage), state) =>
        HeroState(newMessage, LocalDateTime.now().toString)

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

case class UseGreetingMessage(message: String) extends HeroCommand[Done]

object UseGreetingMessage {
  implicit val format: Format[UseGreetingMessage] = Json.format
}

case class GreetingMessageChanged(message: String) extends HeroEvent

object GreetingMessageChanged {
  implicit val format: Format[GreetingMessageChanged] = Json.format
}

object JusticeLeagueSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UseGreetingMessage],
    JsonSerializer[Hero],
    JsonSerializer[GreetingMessageChanged],
    JsonSerializer[HeroState]
  )
}