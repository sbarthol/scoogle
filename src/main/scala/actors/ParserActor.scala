package actors

import actors.ParserActor._
import akka.actor.{Actor, ActorRef}
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters._

class ParserActor(levelDBActor: ActorRef) extends Actor {

  private val minimumWordLength = 3

  override def receive: Receive = { case Body(link, html) =>
    context.parent ! SchedulerActor.NewLinks(link, getLinks(html))
    val text = getText(html)
    levelDBActor ! LevelDBActor.Put(
      words = getWords(text),
      link = link,
      text = text,
      title = getTitle(html)
    )
  }

  private def getTitle(html: String): String = {

    Jsoup.parse(html).title()
  }

  private def getText(html: String): String = {

    Jsoup
      .parse(html)
      .body()
      .text()
  }

  private def getWords(text: String): List[String] = {

    // Todo: reduce to basic form: shoes -> shoe, ate -> eat
    text
      .split("[\\p{Punct}\\s]+") // Todo does not work with " and '
      .toList
      .filter(_.length >= minimumWordLength)
      .map(_.toLowerCase)
      .distinct
  }

  private def getLinks(html: String): List[String] = {
    Jsoup
      .parse(html)
      .select("a[href]")
      .iterator()
      .asScala
      .map(_.absUrl("href"))
      .toList
      .distinct
  }
}

object ParserActor {
  case class Body(link: String, html: String)
}
