package actors

import actors.LinkCheckerActor.Check
import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable

class LinkCheckerActor extends Actor with ActorLogging {

  private val downloaded = new mutable.HashSet[String]()

  override def receive: Receive = { case Check(link) =>
    if (!downloaded.contains(link)) {

      log.debug(s"Link $link not downloaded yet")
      downloaded.add(link)
      sender ! SchedulerActor.CheckedLink(link)
    } else {
      log.debug(s"Link $link has already been downloaded")
    }
  }
}

object LinkCheckerActor {

  case class Check(link: String)
}
