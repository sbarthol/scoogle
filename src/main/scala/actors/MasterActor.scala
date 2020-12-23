package actors

import actors.MasterActor._
import akka.actor.{Actor, Props}
import client.NettyClient
import org.slf4j.LoggerFactory
import source.Source

import scala.collection.mutable

class MasterActor(
    sources: List[Source],
    databaseDirectory: String,
    maxConcurrentSockets: Int
) extends Actor {

  private val downloading = new mutable.HashSet[String]
  private val levelDBActor = context.actorOf(
    props = Props(
      new LevelDBActor(
        invertedIndexFilepath = databaseDirectory + "/invertedIndexDb",
        textFilepath = databaseDirectory + "/textDb"
      )
    ),
    name = "levelDB"
  )
  private val getterActor =
    context.actorOf(
      props = Props(
        new GetterActor(
          client = NettyClient.client,
          maxConcurrentConnections = maxConcurrentSockets
        )
      ),
      name = "getter"
    )

  context.actorOf(
      props = Props(
        new MonitorActor
      ),
      name = "monitor"
    )

  private val logger = LoggerFactory.getLogger(classOf[MasterActor])

  sources.foreach(source => {
    context.actorOf(
      props = Props(
        new SchedulerActor(
          source = source.link,
          maxDepth = source.depth,
          levelDBActor = levelDBActor,
          getterActor = getterActor
        )
      )
    )
  })
  private var downloaded = 0
  logger.debug(s"Started webcrawler for the following sources: $sources")

  override def receive: Receive = {

    case Status =>
      sender ! MonitorActor.Status(
        downloading = downloading.size,
        downloadsCompleted = downloaded
      )

    case Inside(link) =>
      sender ! downloading.contains(link)

    case Put(link) =>
      downloading.add(link)

    case Remove(link) =>
      downloading.remove(link)

    case Increment =>
      downloaded = downloaded + 1
  }
}

object MasterActor {

  case class Inside(link: String)
  case class Put(link: String)
  case class Remove(link: String)
  case object Increment
  case object Status
}
