package actors

import actors.DBActor._
import akka.actor.Actor
import org.slf4j.LoggerFactory
import utils.HBaseConnection

import java.net.URI
import java.security.MessageDigest
import scala.math.{ceil, max}

class DBActor(
    zooKeeperAddress: String,
    zooKeeperPort: Int
) extends Actor {

  // Todo: test those numbers
  private val maxLinksPerPage = 10
  private val maxTitleLength = 50
  private val maxTextLength = 300

  private val logger = LoggerFactory.getLogger(classOf[DBActor])

  private val hbaseConn =
    HBaseConnection.init(
      zooKeeperAddress = zooKeeperAddress,
      zooKeeperPort = zooKeeperPort
    )

  sys.addShutdownHook {
    hbaseConn.close()
    logger.debug("Database was shut down")
  }

  private def cleanLink(link: String): String = {

    val uri = new URI(link)
    new URI(
      uri.getScheme,
      uri.getAuthority,
      uri.getPath,
      null,
      uri.getFragment
    ).toString
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
      logger.debug(s"Link $link put in database")

    case GetLinks(words: List[String], pageNumber) =>
      val hashes = hbaseConn
        .getHashes(words)
        .groupMapReduce { case (hash, _) => hash } { case (_, count) => count }(_ + _)
        .toList
        .sortBy { case (_, count) => -count }

      logger.debug(s"Found a total of ${hashes.size} links")
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
            title = title.take(maxTitleLength),
            text = text.take(maxTextLength),
            cleanLink = cleanLink(link)
          )
        }

      sender ! Response(links = slice, totalPages = totalPages)
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
