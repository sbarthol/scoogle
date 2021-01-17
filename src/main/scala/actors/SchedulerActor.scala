package actors

import actors.SchedulerActor._
import akka.actor.{Actor, ActorRef, Props}
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory

import scala.collection.mutable

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
          distanceToSource.getOrElse(key = newLink, default = maxDepth + 1)

        if (
          urlValidator.isValid(newLink)
          && parentDistanceToSource + 1 <= maxDepth
          && parentDistanceToSource + 1 < childDistanceToSource
        ) {
          distanceToSource.put(
            key = newLink,
            value = parentDistanceToSource + 1
          )

          logger.debug(s"Downloading new Link($newLink)")
          context.parent ! MasterActor.Put(newLink)
          getterActor ! GetterActor.Link(newLink)
        }
      })
  }
}
