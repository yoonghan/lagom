package com.walcron.justiceleague.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import com.walcron.justiceleague.api.JusticeLeagueService

class JusticeLeagueLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new HeroApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HeroApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[JusticeLeagueService])
}

abstract class HeroApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[JusticeLeagueService](wire[JusticeLeagueImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = JusticeLeagueSerializerRegistry

  // Register the hello-lagom persistent entity
  persistentEntityRegistry.register(wire[JusticeLeagueEntity])
}