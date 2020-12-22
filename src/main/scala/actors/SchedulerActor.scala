package actors

import actors.SchedulerActor._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

object SchedulerActor {

  private val urlValidator = new UrlValidator(Array("http", "https"))

  // Todo: make thread safe
  private val downloading = new mutable.HashSet[String]
  private var downloaded = 0

  case class Done(link: String, body: String)
  case class Error(link: String, error: Throwable)
  case class NewLinks(link: String, newLinks: List[String])
}

class SchedulerActor(
    source: String,
    maxDepth: Int,
    levelDBActor: ActorRef,
    getterActor: ActorRef
) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[SchedulerActor])
  private val distanceToSource = new mutable.HashMap[String, Int]

  private val parserActor =
    context.system.actorOf(
      props = Props(
        new ParserActor(levelDBActor = levelDBActor)
      )
    )

  if (!urlValidator.isValid(source)) {
    logger.warn(s"link $source not valid")
  } else if (downloading.contains(source)) {
    logger.warn(s"link $source is already being downloaded")
  } else if (isInDB(source)) {
    logger.warn(s"link $source is already in the database")
  } else {

    logger.debug(s"Downloading new Link($source)")
    distanceToSource.put(source, 0)
    downloading.add(source)

    getterActor ! GetterActor.Link(source)
  }

  override def receive: Receive = {

    case SchedulerActor.Done(link, body) =>
      logger.debug(s"Received Done($link)")
      downloading.remove(link)
      downloaded = downloaded + 1
      logger.info(s"Downloading: ${downloading.size}\nDownloaded: $downloaded")
      parserActor ! ParserActor.Body(link, body)

    case SchedulerActor.Error(link, error) =>
      downloading.remove(link)
      logger.warn(s"Get request for link $link failed: ${error.toString}")

    case NewLinks(link, newLinks) =>
      newLinks.foreach(newLink => {

        val parentDistanceToSource = distanceToSource(link)
        val childDistanceToSource =
          distanceToSource.getOrElse(key = newLink, default = maxDepth + 1)

        if (
          urlValidator.isValid(newLink)
          && !downloading.contains(newLink)
          && !isInDB(newLink)
          && parentDistanceToSource + 1 <= maxDepth
          && parentDistanceToSource + 1 < childDistanceToSource
        ) {
          distanceToSource.put(
            key = newLink,
            value = parentDistanceToSource + 1
          )

          logger.debug(s"Downloading new Link($newLink)")
          downloading.add(newLink)
          getterActor ! GetterActor.Link(newLink)
        }
      })

    case _: Boolean =>
      logger.warn(s"Received a dead letter. Probably a from a isInDb that timed out")
  }

  private def isInDB(link: String): Boolean = {

    implicit val ec: ExecutionContext = context.dispatcher
    val duration = FiniteDuration(5, SECONDS) // Todo: magic number
    implicit val timeout: Timeout = Timeout(duration)
    val future =
      (levelDBActor ? LevelDBActor.Inside(link)).asInstanceOf[Future[Boolean]]

    try {
      Await.result(awaitable = future, atMost = duration)
    } catch {
      case e: Exception =>
        logger.warn(s"isInDb future failed: ${e.toString}")
        false
    }
  }
}
