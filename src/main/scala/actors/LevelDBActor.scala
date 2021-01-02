package actors

import actors.LevelDBActor._
import akka.actor.{Actor, Props}
import org.fusesource.leveldbjni.JniDBFactory.factory
import org.iq80.leveldb.Options
import org.slf4j.LoggerFactory

import java.io._
import scala.collection.mutable
import scala.language.implicitConversions

class LevelDBActor(
    invertedIndexFilepath: String,
    textFilepath: String,
    titleFilepath: String
) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[LevelDBActor])
  private val options = new Options
  options.createIfMissing(true)

  private val invertedIndexDb =
    factory.open(new File(invertedIndexFilepath), options)
  private val textDb = factory.open(new File(textFilepath), options)
  private val titleDb = factory.open(new File(titleFilepath), options)

  sys.addShutdownHook {
    invertedIndexDb.close()
    textDb.close()
    titleDb.close()
    logger.debug("Database was shut down")
  }

  private val putActor = context.actorOf(
    props = Props(new PutActor),
    name = "levelDB.put"
  )

  override def receive: Receive = {

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
        val matchingLinks: List[String] = if (raw == null) List() else raw
        matchingLinks.foreach(link =>
          linkMap.put(key = link, value = linkMap.getOrElse(key = link, default = 0) + 1)
        )
      })

      sender ! linkMap.toList.sortBy(-_._2).take(20)
        .map(tuple => {

          val title: String = titleDb.get(tuple._1)
          val text: String = textDb.get(tuple._1)
          val link: String = tuple._1

          Item(
            link = link,
            title = title.take(50), // Todo: test those numbers
            score = tuple._2,
            text = text.take(300)
          )
        })
  }

  private class PutActor extends Actor {

    override def receive: Receive = { case Put(words, link, text, title) =>
      if (textDb.get(link) == null) {
        textDb.put(link, text)
        titleDb.put(link, title)
        words.foreach(word => {

          val rawLinks = invertedIndexDb.get(word)
          val links: List[String] = if (rawLinks == null) List() else rawLinks
          invertedIndexDb.put(word, link :: links)

        })
        logger.debug(s"Link $link put in database")
      } else {
        logger.debug(s"Link $link already present in the database")
      }
    }
  }

  implicit private def serializeString(s: String): Array[Byte] = s.getBytes
  implicit private def serializeList(l: List[String]): Array[Byte] = {

    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(l)
    oos.close()
    stream.toByteArray
  }

  implicit private def deserializeString(b: Array[Byte]): String = new String(b)
  implicit private def deserializeList(b: Array[Byte]): List[String] = {

    val ois = new ObjectInputStream(new ByteArrayInputStream(b))
    val value = ois.readObject.asInstanceOf[List[String]]
    ois.close()
    value
  }
}

object LevelDBActor {

  case class Put(words: List[String], link: String, text: String, title: String)
  case class Inside(link: String)
  case class GetLinks(words: List[String])
  case class Item(link: String, title: String, score: Int, text: String)
}
