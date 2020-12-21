package actors

import actors.SchedulerActor.{NewLinks, urlValidator}
import akka.actor.{Actor, ActorRef}
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory

import scala.collection.mutable

object SchedulerActor {

  private val urlValidator = new UrlValidator(Array("http", "https"))

  case class Done(link: String, body: String)
  case class Error(link: String, error: Throwable)
  case class NewLinks(link: String, newLinks: List[String])
}

class SchedulerActor(
    source: String,
    maxDepth: Int,
    getterActor: ActorRef,
    parserActor: ActorRef
) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[SchedulerActor])
  private val distanceToSource = new mutable.HashMap[String, Int]
  private val downloading = new mutable.HashSet[String]

  if (urlValidator.isValid(source) && !downloading.contains(source)) {

    logger.debug(s"Downloading new Link($source)")
    distanceToSource.put(source, 0)
    downloading.add(source)

    getterActor ! GetterActor.Link(source)

  } else {
    logger.warn(s"link $source not valid")
  }

  override def receive: Receive = {

    case SchedulerActor.Done(link, body) =>
      logger.debug(s"Received Done($link)")
      downloading.remove(link)

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
  }
}
