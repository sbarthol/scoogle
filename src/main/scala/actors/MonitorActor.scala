package actors

import actors.MonitorActor.Status
import akka.actor.Actor
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, SECONDS}

class MonitorActor extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[MonitorActor])

  private val duration = FiniteDuration(3, SECONDS)
  private implicit val ec: ExecutionContextExecutor = context.dispatcher
  context.system.scheduler.scheduleAtFixedRate(
    duration,
    duration,
    receiver = context.parent,
    message = MasterActor.Status
  )

  override def receive: Receive = { case Status(downloading, completed, failed) =>
    logger.info(
      s"\nDownloading: $downloading\nCompleted Downloads: $completed\nFailed Downloads: $failed"
    )
  }
}

object MonitorActor {

  case class Status(downloading: Int, completed: Int, failed: Int)
}
