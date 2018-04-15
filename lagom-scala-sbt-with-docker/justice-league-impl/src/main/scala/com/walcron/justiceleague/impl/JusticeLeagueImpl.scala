package com.walcron.justiceleague.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.walcron.justiceleague.api.JusticeLeagueService

class JusticeLeagueImpl(persistentEntityRegistry: PersistentEntityRegistry) extends JusticeLeagueService {
  
  override def callHero(id: String) = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[JusticeLeagueEntity](id)
    ref.ask(Hero(id))
  }
  
}