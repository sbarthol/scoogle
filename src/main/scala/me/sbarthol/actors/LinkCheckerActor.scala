package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging}
import me.sbarthol.actors.LinkCheckerActor.Check

import scala.collection.mutable

class LinkCheckerActor extends Actor with ActorLogging {

  private val downloaded = new mutable.HashSet[String]()

  override def receive: Receive = { case Check(links) =>

    val notContained = links.filterNot(downloaded.contains)
    downloaded.addAll(notContained)
    sender ! SchedulerActor.CheckedLinks(notContained)

    log.debug(s"Links $notContained not downloaded yet")
  }
}

object LinkCheckerActor {

  case class Check(links: List[String])
}
