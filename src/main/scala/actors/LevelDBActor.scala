package actors

import actors.LevelDBActor._
import akka.actor.{Actor, Props}
import org.fusesource.leveldbjni.JniDBFactory.factory
import org.iq80.leveldb.Options
import org.slf4j.LoggerFactory

import java.io._
import java.net.URI
import scala.collection.mutable
import scala.language.implicitConversions

class LevelDBActor(
    databaseDirectory: String
) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[LevelDBActor])
  private val options = new Options
  options.createIfMissing(true)

  private val invertedIndexDb =
    factory.open(new File(databaseDirectory + "/invertedIndex"), options)
  private val textDb = factory.open(new File(databaseDirectory + "/text"), options)
  private val titleDb = factory.open(new File(databaseDirectory + "/title"), options)
  private val blacklistDb =
    factory.open(new File(databaseDirectory + "/blacklist"), options)

  sys.addShutdownHook {
    invertedIndexDb.close()
    textDb.close()
    titleDb.close()
    blacklistDb.close()
    logger.debug("Database was shut down")
  }

  private val putActor = context.actorOf(
    props = Props(new PutActor),
    name = "put"
  )

  override def receive: Receive = {

    case IsBlacklisted(link) =>
      val inside = blacklistDb.get(link) != null
      logger.debug(s"Link $link is ${if (inside) "" else "not "}inside the blacklist")
      sender ! inside

    case Blacklist(link) =>
      putActor ! Blacklist(link)

    case Put(words, link, text, title) =>
      putActor ! Put(words, link, text, title)

    case Inside(link: String) =>
      val inside = titleDb.get(link) != null
      logger.debug(s"Link $link is ${if (inside) "" else "not "}inside the database")
      sender ! inside

    case GetLinks(words: List[String]) =>
      val linkMap = mutable.HashMap[String, Int]()

      words.foreach(word => {

        val raw = invertedIndexDb.get(word)
        val matchingLinks: List[(String, Int)] = if (raw == null) List() else raw

        matchingLinks.foreach { case (link, count) =>

          logger.debug(s"The word $word is contained $count times in link $link")
          linkMap.put(
            key = link,
            value = linkMap.getOrElse(key = link, default = 0) + count
          )
        }
      })

      sender ! linkMap.toList
        .sortBy(-_._2)
        .take(20)
        .map { case (link, score) =>
          val title: String = titleDb.get(link)
          val text: String = textDb.get(link)

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
            title = title.take(50), // Todo: test those numbers
            score = score,
            text = text.take(300),
            cleanLink = cleanLink
          )
        }
  }

  private class PutActor extends Actor {

    override def receive: Receive = {

      case Blacklist(link) =>
        blacklistDb.put(link, "")

      case Put(words, link, text, title) =>
        if (textDb.get(link) == null) {
          textDb.put(link, text)
          titleDb.put(link, title)

          words.foreach { case (word, count) =>
            val rawLinks = invertedIndexDb.get(word)
            val links: List[(String, Int)] = if (rawLinks == null) List() else rawLinks
            invertedIndexDb.put(word, (link, count) :: links)
          }

          logger.debug(s"Link $link put in database")
        } else {
          logger.debug(s"Link $link already present in the database")
        }
    }
  }

  implicit private def serializeString(s: String): Array[Byte] = s.getBytes
  implicit private def serializeList[A](l: List[A]): Array[Byte] = {

    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(l)
    oos.close()
    stream.toByteArray
  }

  implicit private def deserializeString(b: Array[Byte]): String = new String(b)
  implicit private def deserializeList[A](b: Array[Byte]): List[A] = {

    val ois = new ObjectInputStream(new ByteArrayInputStream(b))
    val value = ois.readObject.asInstanceOf[List[A]]
    ois.close()
    value
  }
}

object LevelDBActor {

  case class Put(words: List[(String, Int)], link: String, text: String, title: String)
  case class Blacklist(link: String)
  case class IsBlacklisted(link: String)
  case class Inside(link: String)
  case class GetLinks(words: List[String])
  case class Item(cleanLink: String, link: String, title: String, score: Int, text: String)
}
