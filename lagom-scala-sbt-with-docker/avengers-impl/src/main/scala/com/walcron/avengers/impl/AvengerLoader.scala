package com.walcron.avengers.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import com.walcron.justiceleague.api.AvengersService
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
 * Implementation of the loader, which needs to be defined in build.sbt.
 * All dependency injection are triggered here.
 */
class AvengerLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new AvengersApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AvengersApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[AvengersService])
}

abstract class AvengersApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with PubSubComponents
    with LagomKafkaComponents 
    with CassandraPersistenceComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[AvengersService](wire[AvengersServiceImpl])
  
  override lazy val jsonSerializerRegistry = AvengersRegistry
  
  lazy val avengersService = serviceClient.implement[AvengersService]
  
  persistentEntityRegistry.register(wire[AvengersEntity])
  
  lazy val repository: AvengersReadSide = wire[AvengersReadSide]
  readSide.register(wire[AvengersReadSide])
}
