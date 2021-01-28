package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import me.sbarthol.actors.ParserActor.{Body, extractWords}
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters._

class ParserActor(dbActor: ActorRef) extends Actor with ActorLogging {

  private val minimumElementTextLength = 10

  override def receive: Receive = { case Body(link, html) =>
    context.parent ! SchedulerActor.NewLinks(link, getLinks(html, link))

    val text = getText(html)
    val title = getTitle(html)
    val words = getWords(title = title, text = text)

    log.debug(s"Link $link contains title $title text $text")

    if (words.nonEmpty) {

      dbActor ! DBActor.Put(
        words = words,
        link = link,
        text = text,
        title = title
      )
    }
  }

  private def getTitle(html: String): String = {

    val doc = Jsoup.parse(html)
    val h1 = doc.select("h1")
    val title = doc.title()

    if (!h1.isEmpty) h1.get(0).text()
    else if (title.nonEmpty) title
    else "No Title Found"
  }

  private def getText(html: String): String = {

    try {

      Jsoup
        .parse(html)
        .getAllElements
        .textNodes()
        .asScala
        .map(_.text)
        .filter(_.length >= minimumElementTextLength)
        .mkString(" ")

    } catch {
      case _: Exception => ""
    }
  }

  private def getWords(
      title: String,
      text: String
  ): List[(String, Int)] = {

    val titleWords = extractWords(title)
      .groupBy(identity)
      .view
      .mapValues(_.size * 4)
      .toList

    // Todo: reduce to basic form: shoes -> shoe, ate -> eat
    val textWords = extractWords(text)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toList

    (titleWords ++ textWords).groupMapReduce { case (word, _) =>
      word
    } { case (_, count) =>
      count
    }(_ + _).toList
  }

  private def getLinks(html: String, link: String): List[String] = {

    Jsoup
      .parse(html, link)
      .select("a")
      .asScala
      .map(_.absUrl("href"))
      .filter(_.nonEmpty)
      .toList
      .distinct
  }
}

object ParserActor {
  def extractWords(text: String): List[String] = {

    val minimumWordLength = 3

    text
      .split(
        "[[ ]*|[,]*|[;]*|[:]*|[']*|[’]*|[\\\\]*|[\"]*|[.]*|[…]*|[:]*|[/]*|[!]*|[?]*|[+]*]+"
      )
      .toList
      .filter(word => word.length >= minimumWordLength && word.forall(_.isLetter))
      .map(_.toLowerCase)
  }

  case class Body(link: String, html: String)
}