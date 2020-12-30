package actors

import actors.FindActor.Find
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success}

class FindActor(databaseDirectory: String) extends Actor {

  private val logger = LoggerFactory.getLogger(classOf[FindActor])

  private val levelDBActor = context.actorOf(
    props = Props(
      new LevelDBActor(
        invertedIndexFilepath = databaseDirectory + "/invertedIndexDb",
        textFilepath = databaseDirectory + "/textDb"
      )
    ),
    name = "levelDB"
  )

  override def receive: Receive = { case Find(words) =>

    implicit val timeout: Timeout =
      Timeout(duration = FiniteDuration(5, SECONDS))
    val future =
      (levelDBActor ? LevelDBActor.GetLinks(words = words)).mapTo[List[String]]

    implicit val ec: ExecutionContext = context.dispatcher
    future
      .map(
        _.groupBy(identity).view
          .mapValues(_.size)
          .toList
          .sortBy(-_._2)
      )
      .onComplete {
        case Success(value) => {
          sender ! value
        }
        case Failure(exception) =>
          logger.error(s"Error getting links from database: ${exception.toString}")
      }
  }
}

object FindActor {

  case class Find(words: List[String])
}
