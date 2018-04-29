package com.walcron.avengers.impl

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import scala.concurrent.ExecutionContext
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import scala.concurrent.Future
import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.walcron.avengers.api.Hero
import com.datastax.driver.core.BoundStatement

/**
 * Adding print statement to display the creation of ReadSide,
 * Readside is wired in AvengerLoader and triggered automatically after onEvent.
 */
class AvengersReadSide(db: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
extends ReadSideProcessor[AvengersTimelineEvent] {

  private var insertChirp: PreparedStatement = _

  /**A bug over here, as because it should execute as a ref not value**/
  override def buildHandler() = readSide.builder[AvengersTimelineEvent]("AvengersReadSide")
    .setGlobalPrepare(() => createTable)
    .setPrepare(_ => prepareInsert())
    .setEventHandler[HeroAdded](ese => insertRecord(ese.event.hero))
    .build()

  override def aggregateTags = AvengersTimelineEvent.Tag.allTags

  private def createTable() = {
   println(">>Creating table if not exist")
   db.executeCreateTable(
      """CREATE TABLE IF NOT EXISTS avengers
       | (userId text, timestamp bigint, uuid text, message text,
       | PRIMARY KEY (userId, timestamp, uuid))
       |""".stripMargin
       )
  }

  private def prepareInsert(): Future[Done] = {
    println(">>Preparing db statement")
    db.prepare("INSERT INTO avengers (userId, uuid, timestamp, message) VALUES (?, ?, ?, ?)")
      .map { ps =>
        insertChirp = ps
        Done
      }
  }

  private def insertRecord(hero: Hero): Future[List[BoundStatement]] = {
    println(">>Inserting database records: "+hero.userId)
    val bindInsertChirp = insertChirp.bind()
    bindInsertChirp.setString("userId", hero.userId)
    bindInsertChirp.setString("uuid", hero.uuid)
    bindInsertChirp.setLong("timestamp", hero.timestamp.toEpochMilli)
    bindInsertChirp.setString("message", hero.message)
    Future.successful(List(bindInsertChirp))
  }

  def readHeroes(): Future[Seq[Hero]] =
    db.selectAll("SELECT * FROM avengers")
      .map {
        optRow =>
          optRow.map{
            row =>
              val userId = row.getString("userId")
              new Hero(userId, "")
          }
      }
}
