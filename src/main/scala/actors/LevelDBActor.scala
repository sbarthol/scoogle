package actors

import actors.LevelDBActor._
import akka.actor.{Actor, Props}
import org.fusesource.leveldbjni.JniDBFactory.factory
import org.iq80.leveldb.{DB, Options}
import org.slf4j.LoggerFactory

import java.io._
import scala.language.implicitConversions

class LevelDBActor(invertedIndexFilepath: String, textFilepath: String) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[LevelDBActor])

  private val options = new Options
  options.createIfMissing(true)

  private val invertedIndexDb =
    factory.open(new File(invertedIndexFilepath), options)
  private val textDb = factory.open(new File(textFilepath), options)

  sys.addShutdownHook {
    invertedIndexDb.close()
    textDb.close()
    logger.debug("Database was shut down")
  }

  private val putActor = context.actorOf(
    props = Props(new PutActor(invertedIndexDb = invertedIndexDb, textDb = textDb)),
    name = "levelDB.put"
  )

  override def receive: Receive = {

    case Put(words, link, text) =>
      putActor ! Put(words, link, text)

    case Inside(link: String) =>
      val inside = textDb.get(link) != null
      logger.debug(s"Link $link is ${if (inside) "" else "not "}inside the database")
      sender ! inside
  }

  private class PutActor(invertedIndexDb: DB, textDb: DB) extends Actor {

    override def receive: Receive = { case Put(words, link, text) =>
      textDb.put(link, text)
      words.foreach(word => {

        val rawLinks = invertedIndexDb.get(word)
        val links: List[String] = if (rawLinks == null) List() else rawLinks
        invertedIndexDb.put(word, link :: links)

      })
      logger.debug(s"Link $link put in database")
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

  case class Put(words: List[String], link: String, text: String)
  case class Inside(link: String)
}
