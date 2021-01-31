package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import me.sbarthol.actors.SchedulerActor.{CheckedLinks, Done, Error, NewLinks}

import java.net.URL
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object SchedulerActor {

  case class Done(link: String, body: String)
  case class Error(link: String, error: Throwable)
  case class NewLinks(link: String, newLinks: List[String])
  case class CheckedLinks(link: List[String])
}

class SchedulerActor(
    source: String,
    maxDepth: Int,
    parserActorManager: ActorRef,
    getterActor: ActorRef,
    linkCheckerActor: ActorRef
) extends Actor
    with ActorLogging {

  private val distanceToSource = new mutable.HashMap[String, Int]

  if (maxDepth < 0) {
    log.error(s"maxDepth $maxDepth is smaller than 0")
    context.stop(self)
  } else {
    linkCheckerActor ! LinkCheckerActor.Check(List(source))
    distanceToSource.put(source, 0)
  }

  override def receive: Receive = {

    case Done(link, body) =>
      log.debug(s"Received Done($link)")
      context.parent ! MasterActor.Remove
      context.parent ! MasterActor.Increment
      parserActorManager ! ParserActor.Body(link, body, context.self)

    case Error(link, error) =>
      context.parent ! MasterActor.Remove
      context.parent ! MasterActor.Error
      log.warning(s"Get request for link $link failed: ${error.toString}")

    case NewLinks(link, newLinks) =>
      val parentDistanceToSource = distanceToSource(link)

      val validNewLinks = newLinks.filter(newLink => {

        val childDistanceToSource =
          distanceToSource.getOrElse(key = newLink, default = maxDepth + 1)

        sameHost(
          source,
          newLink
        ) && parentDistanceToSource + 1 <= maxDepth && parentDistanceToSource + 1 < childDistanceToSource
      })

      validNewLinks.foreach(newLink =>
        distanceToSource.put(key = newLink, value = parentDistanceToSource + 1)
      )
      linkCheckerActor ! LinkCheckerActor.Check(validNewLinks)

    case CheckedLinks(links) =>
      log.debug(s"Downloading new Links($links)")
      context.parent ! MasterActor.Put(links.size)
      getterActor ! GetterActor.Links(links)
  }

  private def sameHost(first: String, second: String): Boolean = {

    Try {
      val firstUrl = new URL(first)
      val secondUrl = new URL(second)
      firstUrl.getHost == secondUrl.getHost
    } match {
      case Success(sameHost) => sameHost
      case Failure(_) =>
        log.warning(s"Could not determine whether $first and $second have the same host")
        false
    }
  }
}
