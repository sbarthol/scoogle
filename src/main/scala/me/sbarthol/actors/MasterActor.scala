package me.sbarthol.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.routing.RoundRobinPool
import me.sbarthol.actors.MasterActor.{Error, Increment, Put, Remove, Status}
import me.sbarthol.source.Source
import me.sbarthol.utils.{HBaseConnection, NettyClient}

class MasterActor(
    sources: List[Source],
    zooKeeperAddress: String,
    zooKeeperPort: Int,
    maxConcurrentSockets: Int
) extends Actor
    with ActorLogging {

  private val hbaseConn =
    HBaseConnection.init(
      zooKeeperAddress = zooKeeperAddress,
      zooKeeperPort = zooKeeperPort
    )

  sys.addShutdownHook {
    hbaseConn.close()
    log.debug("Database was shut down")
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, loggingEnabled = true) { case _: Exception =>
      Stop
    }

  private val dbActorManager =
    context.actorOf(
      RoundRobinPool(10).props(
        Props(new DBActor(hbaseConn))
      ),
      "DBActorManager"
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
          dbActorManager = dbActorManager,
          getterActor = getterActor,
          linkCheckerActor = linkCheckerActor
        )
      )
    )
  })

  private var downloading = 0
  private var completed = 0
  private var failed = 0

  log.debug(s"Started web crawler for the following sources: $sources")

  override def receive: Receive = {

    case Status =>
      sender ! MonitorActor.Status(
        downloading = downloading,
        completed = completed,
        failed = failed
      )

    case Put(n) =>
      downloading = downloading + n

    case Remove =>
      downloading = downloading - 1

    case Increment =>
      completed = completed + 1

    case Error =>
      failed = failed + 1
  }
}

object MasterActor {

  case class Put(n: Int)
  case object Remove
  case object Error
  case object Increment
  case object Status
}
