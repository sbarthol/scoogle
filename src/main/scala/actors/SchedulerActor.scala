package actors

import actors.SchedulerActor._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, blocking}

object SchedulerActor {

  private val urlValidator = new UrlValidator(Array("http", "https"))

  case class Done(link: String, body: String)
  case class Error(link: String, error: Throwable)
  case class NewLinks(link: String, newLinks: List[String])

  class InitializationException(message: String) extends Exception(message)
  class DownloadSourceException(message: String) extends Exception(message)
}

class SchedulerActor(
    source: String,
    maxDepth: Int,
    crawlPresentLinks: Boolean,
    dbActor: ActorRef,
    getterActor: ActorRef
) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[SchedulerActor])
  private val distanceToSource = new mutable.HashMap[String, Int]
  private val parserActor =
    context.actorOf(
      props = Props(
        new ParserActor(dbActor = dbActor)
      )
    )

  logger.debug(s"Attempt to start new scheduler actor for source: $source")

  try {
    if (maxDepth < 0) {
      throw new InitializationException(s"maxDepth $maxDepth is smaller than 0")
    } else if (!urlValidator.isValid(source)) {
      throw new InitializationException(s"link $source not valid")
    } else if (isBlacklisted(source)) {
      throw new InitializationException(s"link $source is blacklisted")
    } else if (isDownloading(source)) {
      throw new InitializationException(s"link $source is already being downloaded")
    } else if (!crawlPresentLinks && isInDB(source)) {
      throw new InitializationException(s"link $source is already in the database")
    } else {

      logger.debug(s"Downloading new Link($source)")
      distanceToSource.put(source, 0)
      context.parent ! MasterActor.Put(source)
      getterActor ! GetterActor.Link(source)
    }
  } catch {
    case e: Throwable =>
      logger.error(s"Error when initializing a scheduler: ${e.getMessage}")
  }

  override def receive: Receive = {

    case SchedulerActor.Done(link, body) =>
      logger.debug(s"Received Done($link)")
      context.parent ! MasterActor.Remove(link)
      context.parent ! MasterActor.Increment
      parserActor ! ParserActor.Body(link, body)

    case SchedulerActor.Error(link, error) =>
      context.parent ! MasterActor.Remove(link)
      val errorDescription = s"Get request for link $link failed: ${error.toString}"
      if (source == link) {
        logger.warn(s"Failed downloading a source: $errorDescription")
        throw new DownloadSourceException(errorDescription)
      } else {
        context.parent ! MasterActor.Error(link)
        logger.warn(errorDescription)
      }

    case NewLinks(link, newLinks) =>
      newLinks.foreach(newLink => {

        val parentDistanceToSource = distanceToSource(link)
        val childDistanceToSource =
          distanceToSource.synchronized {
            distanceToSource.getOrElse(key = newLink, default = maxDepth + 1)
          }

        blocking {

          if (
            urlValidator.isValid(newLink)
            && parentDistanceToSource + 1 <= maxDepth
            && parentDistanceToSource + 1 < childDistanceToSource
            && !isDownloading(newLink)
            && !isBlacklisted(newLink)
            && (crawlPresentLinks || !isInDB(newLink))
          ) {
            distanceToSource.synchronized {
              distanceToSource.put(
                key = newLink,
                value = parentDistanceToSource + 1
              )
            }

            logger.debug(s"Downloading new Link($newLink)")
            context.parent ! MasterActor.Put(newLink)
            getterActor ! GetterActor.Link(newLink)
          }
        }
      })
  }

  private def isBlacklisted(link: String): Boolean = {

    implicit val ec: ExecutionContext = context.dispatcher
    val duration = FiniteDuration(60, SECONDS)
    implicit val timeout: Timeout = Timeout(duration)
    val future =
      (dbActor ? DBActor.IsBlacklisted(link)).mapTo[Boolean]

    try {
      Await.result(awaitable = future, atMost = duration)
    } catch {
      case e: Exception =>
        logger.warn(s"isBlacklisted future failed: ${e.toString}")
        false
    }
  }

  private def isDownloading(link: String): Boolean = {

    implicit val ec: ExecutionContext = context.dispatcher
    val duration = FiniteDuration(60, SECONDS)
    implicit val timeout: Timeout = Timeout(duration)
    val future =
      (context.parent ? MasterActor.Inside(link)).mapTo[Boolean]

    try {
      Await.result(awaitable = future, atMost = duration)
    } catch {
      case e: Exception =>
        logger.warn(s"isDownloading future failed: ${e.toString}")
        false
    }
  }

  private def isInDB(link: String): Boolean = {

    implicit val ec: ExecutionContext = context.dispatcher
    val duration = FiniteDuration(60, SECONDS)
    implicit val timeout: Timeout = Timeout(duration)
    val future =
      (dbActor ? DBActor.Inside(link)).mapTo[Boolean]

    try {
      Await.result(awaitable = future, atMost = duration)
    } catch {
      case e: Exception =>
        logger.warn(s"isInDb future failed: ${e.toString}")
        false
    }
  }
}
