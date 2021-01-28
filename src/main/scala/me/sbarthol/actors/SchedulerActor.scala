package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import me.sbarthol.actors.SchedulerActor.{CheckedLink, Done, Error, NewLinks}

import java.net.URL
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object SchedulerActor {

  case class Done(link: String, body: String)
  case class Error(link: String, error: Throwable)
  case class NewLinks(link: String, newLinks: List[String])
  case class CheckedLink(link: String)
}

class SchedulerActor(
    source: String,
    maxDepth: Int,
    dbActor: ActorRef,
    getterActor: ActorRef,
    linkCheckerActor: ActorRef
) extends Actor
    with ActorLogging {

  private val distanceToSource = new mutable.HashMap[String, Int]
  private val parserActor =
    context.actorOf(
      props = Props(
        new ParserActor(dbActor = dbActor)
      )
    )

  if (maxDepth < 0) {
    log.error(s"maxDepth $maxDepth is smaller than 0")
    context.stop(self)
  } else {
    linkCheckerActor ! LinkCheckerActor.Check(source)
    distanceToSource.put(source, 0)
  }

  override def receive: Receive = {

    case Done(link, body) =>
      log.debug(s"Received Done($link)")
      context.parent ! MasterActor.Remove(link)
      context.parent ! MasterActor.Increment
      parserActor ! ParserActor.Body(link, body)

    case Error(link, error) =>
      context.parent ! MasterActor.Remove(link)
      context.parent ! MasterActor.Error
      log.warning(s"Get request for link $link failed: ${error.toString}")

    case NewLinks(link, newLinks) =>
      newLinks.foreach(newLink => {

        val parentDistanceToSource = distanceToSource(link)
        val childDistanceToSource =
          distanceToSource.getOrElse(key = newLink, default = maxDepth + 1)

        if (
          sameHost(source, newLink)
          && parentDistanceToSource + 1 <= maxDepth
          && parentDistanceToSource + 1 < childDistanceToSource
        ) {

          linkCheckerActor ! LinkCheckerActor.Check(newLink)
          distanceToSource.put(
            key = newLink,
            value = parentDistanceToSource + 1
          )

        } else {
          log.debug(s"Link $newLink does not meet one or more conditions")
        }
      })

    case CheckedLink(link) =>
      log.debug(s"Downloading new Link($link)")
      context.parent ! MasterActor.Put(link)
      getterActor ! GetterActor.Link(link)
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
