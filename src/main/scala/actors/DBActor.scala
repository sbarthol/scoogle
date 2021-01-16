package actors

import actors.DBActor._
import akka.actor.{Actor, Props}
import akka.routing.RoundRobinPool
import org.slf4j.LoggerFactory
import utils.HBaseConnection

import java.net.URI
import scala.collection.mutable
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
  //private val putActor = context.actorOf(Props(new PutActor), "put")
  private val putRouter =
    context.actorOf(RoundRobinPool(2).props(Props(new PutActor)), "putRouter")

  override def receive: Receive = {

    case IsBlacklisted(link) =>
      val inside = hbaseConn.isBlacklisted(link)
      logger.debug(s"Link $link is ${if (inside) "" else "not "}inside the blacklist")
      sender ! inside

    case Blacklist(link) =>
      putRouter ! Blacklist(link)

    case Put(words, link, text, title) =>
      putRouter ! Put(words, link, text, title)

    case Inside(link: String) =>
      val inside = hbaseConn.isInDb(link)
      logger.debug(s"Link $link is ${if (inside) "" else "not "}inside the database")
      sender ! inside

    case GetLinks(words: List[String], pageNumber) =>
      val linkMap = mutable.HashMap[String, Int]()

      words.foreach(word => {

        val raw = ???
        val matchingLinks: List[(String, Int)] = if (raw == null) List() else raw

        matchingLinks.foreach { case (link, count) =>
          logger.debug(s"The word $word is contained $count times in link $link")
          linkMap.put(
            key = link,
            value = linkMap.getOrElse(key = link, default = 0) + count
          )
        }
      })

      logger.debug(s"Found a total of ${linkMap.size} links")

      val totalPages = max(1, ceil(linkMap.size / maxLinksPerPage.toDouble).toInt)
      val links = linkMap.toList
        .sortBy(-_._2)
        .slice(
          from = maxLinksPerPage * (pageNumber - 1),
          until = maxLinksPerPage * pageNumber
        )
        .map { case (link, _) =>
          val title: String = ???
          val text: String = ???

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

      sender ! Response(links = links, totalPages = totalPages)
  }

  private class PutActor extends Actor {

    override def receive: Receive = {

      case Blacklist(link) =>
        hbaseConn.blacklist(link)

      case Put(words, link, text, title) =>
        hbaseConn.putWebsite(link = link, text = text, title = title)
        words.foreach { case (word, count) =>
          hbaseConn.putWord(link = link, word = word, count = count)
        }
        logger.debug(s"Link $link put in database")
    }
  }
}

object DBActor {

  case class Put(words: List[(String, Int)], link: String, text: String, title: String)
  case class Blacklist(link: String)
  case class IsBlacklisted(link: String)
  case class Inside(link: String)
  case class Response(links: List[Item], totalPages: Int)
  case class GetLinks(words: List[String], pageNumber: Int)
  case class Item(
      cleanLink: String,
      link: String,
      title: String,
      text: String
  )
}
