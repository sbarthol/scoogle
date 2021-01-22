package actors

import actors.MasterActor._
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import source.Source
import utils.NettyClient

import scala.collection.mutable

class MasterActor(
    sources: List[Source],
    zooKeeperAddress: String,
    zooKeeperPort: Int,
    maxConcurrentSockets: Int
) extends Actor
    with ActorLogging {

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, loggingEnabled = true) { case _: Exception =>
      Stop
    }

  private val downloading = new mutable.HashSet[String]
  private val dbActor = context.actorOf(
    props = Props(
      new DBActor(zooKeeperAddress = zooKeeperAddress, zooKeeperPort = zooKeeperPort)
    ),
    name = "db"
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

  private val linkCheckerActor =
    context.actorOf(props = Props(new LinkCheckerActor), name = "linkChecker")

  context.actorOf(
    props = Props(
      new MonitorActor
    ),
    name = "monitor"
  )

  sources.foreach(source => {
    context.actorOf(
      props = Props(
        new SchedulerActor(
          source = source.link,
          maxDepth = source.depth,
          dbActor = dbActor,
          getterActor = getterActor,
          linkCheckerActor = linkCheckerActor
        )
      )
    )
  })

  private var completed = 0
  private var failed = 0

  log.debug(s"Started web crawler for the following sources: $sources")

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
  case object Error
  case object Increment
  case object Status
}
