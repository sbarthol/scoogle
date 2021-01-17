package actors

import actors.DBActor._
import akka.actor.Actor
import org.slf4j.LoggerFactory
import utils.HBaseConnection

import java.net.URI
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

  override def receive: Receive = {

    case Put(words, link, text, title) =>
      hbaseConn.putWebsite(link = link, text = text, title = title)
      hbaseConn.putWords(link = link, words = words)
      logger.debug(s"Link $link put in database")

    case GetLinks(words: List[String], pageNumber) =>
      val links = hbaseConn
        .getLinks(words)
        .groupMapReduce { case (link, _) => link } { case (_, count) => count }(_ + _)
        .toList
        .sortBy { case (_, count) => -count }

      logger.debug(s"Found a total of ${links.size} links")
      val totalPages = max(1, ceil(links.size / maxLinksPerPage.toDouble).toInt)

      val slice = links
        .slice(
          from = maxLinksPerPage * (pageNumber - 1),
          until = maxLinksPerPage * pageNumber
        )
        .map { case (link, _) =>
          val (title, text) = hbaseConn.getWebsite(link)
          val uri = new URI(link)
          val cleanLink: String = new URI(
            uri.getScheme,
            uri.getAuthority,
            uri.getPath,
            null,
            uri.getFragment
          ).toString

          Item(
            link = link,
            title = title.take(maxTitleLength),
            text = text.take(maxTextLength),
            cleanLink = cleanLink
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
