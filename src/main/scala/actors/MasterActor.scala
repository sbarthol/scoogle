package actors

import actors.MasterActor._
import actors.SchedulerActor.{DownloadSourceException, InitializationException}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import client.NettyClient
import org.slf4j.LoggerFactory
import source.Source

import scala.collection.mutable

class MasterActor(
    sources: List[Source],
    databaseDirectory: String,
    maxConcurrentSockets: Int,
    overridePresentLinks: Boolean
) extends Actor {

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3) {
      case _: InitializationException => Stop
      case _: DownloadSourceException => Restart
      case _: Exception               => Stop
    }

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
          overridePresentLinks = overridePresentLinks,
          levelDBActor = levelDBActor,
          getterActor = getterActor
        )
      )
    )
  })

  private var completed = 0
  private var failed = 0

  logger.debug(s"Started webcrawler for the following sources: $sources")

  override def receive: Receive = {

    case Status =>
      sender ! MonitorActor.Status(
        downloading = downloading.size,
        completed = completed,
        failed = failed
      )

    case Inside(link) =>
      sender ! downloading.contains(link)

    case Put(link) =>
      downloading.add(link)

    case Remove(link) =>
      downloading.remove(link)

    case Increment =>
      completed = completed + 1

    case Error =>
      failed = failed + 1
  }
}

object MasterActor {

  case class Inside(link: String)
  case class Put(link: String)
  case class Remove(link: String)
  case object Increment
  case object Status
  case object Error
}
