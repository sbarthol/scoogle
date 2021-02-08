package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging}
import me.sbarthol.actors.DBActor._
import me.sbarthol.actors.ParserActor.{highlight, toKeywords}
import me.sbarthol.utils.HBaseConnection

import java.net.URL
import java.security.MessageDigest
import scala.math.{ceil, max}
import scala.util.{Failure, Success, Try}

class DBActor(hbaseConn: HBaseConnection) extends Actor with ActorLogging {

  private val maxLinksPerPage = 10
  private val maxTitleLength = 80
  private val maxItems = 99 * maxLinksPerPage

  override def receive: Receive = {

    case Put(words, link, text, title) =>
      val hash = MessageDigest
        .getInstance("SHA-256")
        .digest(text.getBytes("UTF-8"))
        .map("%02x".format(_))
        .mkString

      hbaseConn.putWebsite(link = link, text = text, title = title, hash = hash)
      hbaseConn.putWords(hash = hash, words = words)
      log.debug(s"Link $link put in database")

    case GetLinks(query: String, pageNumber) =>
      val startMoment = System.currentTimeMillis

      val keywords = toKeywords(query).distinct

      val hashes = keywords match {
        case w if w.isEmpty => List.empty
        case _              => hbaseConn.getHashes(keywords)
      }

      val trimmedHashes = hashes.sortBy { case (_, s) => -computeScore(s) }.take(maxItems)

      log.debug(s"Found a total of ${trimmedHashes.size} links")
      val nPages = max(1, ceil(trimmedHashes.size / maxLinksPerPage.toDouble).toInt)

      val slice = trimmedHashes
        .slice(
          from = maxLinksPerPage * (pageNumber - 1),
          until = maxLinksPerPage * pageNumber
        )
        .map { case (hash, _) =>
          val (title, text, link) = hbaseConn.getWebsite(hash = hash)

          Item(
            link = link.replace("file://", ""),
            title = cleanText(title.take(maxTitleLength)),
            text = cleanText(highlight(text, keywords)),
            cleanLink = cleanLink(link).replace("file:", "")
          )
        }

      val endMoment = System.currentTimeMillis
      sender ! Response(
        links = slice,
        nPages = nPages,
        nResults = hashes.size,
        processingTimeMillis = endMoment - startMoment
      )
  }

  private def computeScore(s: Score): Int = {
    s.minFreq + s.sumFreq
  }

  private def cleanLink(link: String): String = {

    Try {
      val url = new URL(link)
      val noQuery = new URL(
        url.getProtocol,
        url.getHost,
        url.getPath
      ).toString

      noQuery
    } match {
      case Success(cleanLink) => cleanLink
      case Failure(t) =>
        log.warning(s"Error cleaning link: ${t.toString}")
        link
    }
  }

  private def cleanText(text: String): String = {

    text
      .replace('\uFFFD', ' ')
      .replace("\\", "")
      .trim
      .replaceAll(" +", " ")
  }
}

object DBActor {

  case class Put(words: List[(String, Int)], link: String, text: String, title: String)
  case class Response(
      links: List[Item],
      nPages: Int,
      nResults: Int,
      processingTimeMillis: Long
  )
  case class GetLinks(query: String, pageNumber: Int)
  case class Item(
      cleanLink: String,
      link: String,
      title: String,
      text: String
  )

  case class Score(minFreq: Int, sumFreq: Int)
}
