package actors

import actors.DBActor._
import akka.actor.{Actor, ActorLogging}
import utils.HBaseConnection

import java.net.{URI, URLDecoder}
import java.security.MessageDigest
import scala.math.{ceil, max}

class DBActor(
    zooKeeperAddress: String,
    zooKeeperPort: Int
) extends Actor
    with ActorLogging {

  private val maxLinksPerPage = 10
  private val maxTitleLength = 80

  private val hbaseConn =
    HBaseConnection.init(
      zooKeeperAddress = zooKeeperAddress,
      zooKeeperPort = zooKeeperPort
    )

  sys.addShutdownHook {
    hbaseConn.close()
    log.debug("Database was shut down")
  }

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

    case GetLinks(words: List[String], pageNumber) =>
      val hashes = hbaseConn
        .getHashes(words)
        .groupMap {case (hash, _) => hash} {case (_, count) => count}
        .filter {case (_, list) => list.size == words.size}
        .view.mapValues(_.sum)
        .toList
        .sortBy { case (_, count) => -count }

      log.debug(s"Found a total of ${hashes.size} links")
      val totalPages = max(1, ceil(hashes.size / maxLinksPerPage.toDouble).toInt)

      val slice = hashes
        .slice(
          from = maxLinksPerPage * (pageNumber - 1),
          until = maxLinksPerPage * pageNumber
        )
        .map { case (hash, _) =>
          val (title, text, link) = hbaseConn.getWebsite(hash = hash)

          Item(
            link = link,
            title = cleanText(title.take(maxTitleLength)),
            text = cleanText(selectTextSegments(text, words)),
            cleanLink = cleanLink(link)
          )
        }

      sender ! Response(links = slice, totalPages = totalPages)
  }

  private def selectTextSegments(text: String, keywords: List[String]): String = {

    val numberWrappingWords = 4
    val sb = new StringBuilder()
    val words = text.trim.split(" ")

    def addToSb(w: String, bold: Boolean): Unit = {
      if (bold) sb.addAll("<strong>")
      sb.addAll(w)
      if (bold) sb.addAll("</strong>")
    }

    for (i <- 0 until words.length) {
      if (keywords.exists(words(i).toLowerCase.startsWith)) { // Todo: mieux que ca ?

        val start = math.max(0, i - numberWrappingWords)
        val end = math.min(i + numberWrappingWords, words.length - 1)

        for (j <- start until end) {
          addToSb(words(j), i == j)
          sb.addOne(' ')
        }
        addToSb(words(end), i == end)
        sb.addAll("... ")
      }
    }

    sb.toString
  }

  private def cleanLink(link: String): String = {

    val uri = new URI(link)
    val noQuery = new URI(
      uri.getScheme,
      uri.getAuthority,
      uri.getPath,
      null,
      uri.getFragment
    ).toString

    URLDecoder.decode(noQuery, "UTF-8" )
  }

  private def cleanText(text: String): String = {

    text
      .replace('�', ' ')
      .replace("\\", "")
      .trim
      .replaceAll(" +", " ")
  }
}

object DBActor {

  case class Put(words: List[(String, Int)], link: String, text: String, title: String)
  case class Response(links: List[Item], totalPages: Int)
  case class GetLinks(words: List[String], pageNumber: Int)
  case class Item(
      cleanLink: String,
      link: String,
      title: String,
      text: String
  )
}
