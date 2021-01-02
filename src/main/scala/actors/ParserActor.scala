package actors

import actors.ParserActor.{Body, Find, Result}
import akka.actor.Actor
import org.jsoup.Jsoup

import scala.collection.mutable
import scala.jdk.CollectionConverters._

class ParserActor extends Actor {

  private val minimumWordLength = 3
  // Todo: minimum text length ?

  // Todo: move this to another actor
  private val invertedIndex = new mutable.HashMap[String, List[String]]

  override def receive: Receive = {

    case Body(link, html) =>
      sender() ! SchedulerActor.NewLinks(link, getLinks(html))
      getWords(html).foreach(word =>
        invertedIndex.put(word, link :: invertedIndex.getOrElse(word, List()))
      )

    case Find(words) =>
      val links = words
        .flatMap(invertedIndex.getOrElse(_, List()))
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toList
        .sortBy(-_._2)

      println(links) // Todo: remove
      sender() ! Result(links)
  }

  private def getWords(html: String): List[String] = {

    // Todo: reduce to basic form: shoes -> shoe, ate -> eat
    Jsoup
      .parse(html)
      .body()
      .text()
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
  case class Find(words: List[String])
  case class Result(links: List[(String, Int)]) // Todo: not in this class
}
