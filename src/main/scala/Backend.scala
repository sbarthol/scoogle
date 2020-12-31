import actors.LevelDBActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import conf.BackendConf
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val itemFormat: RootJsonFormat[LevelDBActor.Item] = jsonFormat4(
    LevelDBActor.Item
  )
}

object Backend {

  private val logger = LoggerFactory.getLogger(classOf[Backend])

  def main(args: Array[String]): Unit = {

    val conf = new BackendConf(args)
    implicit val system: ActorSystem = ActorSystem("Backend")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val levelDBActor = system.actorOf(
      props = Props(
        new LevelDBActor(
          invertedIndexFilepath = conf.databaseDirectory.apply() + "/invertedIndexDb",
          textFilepath = conf.databaseDirectory.apply() + "/textDb",
          titleFilepath = conf.databaseDirectory.apply() + "/titleDb"
        )
      ),
      name = "levelDB"
    )

    val route = parameters("keyword".repeated, "keyword") {
      implicit val timeout: Timeout = Timeout(FiniteDuration(length = 20, unit = SECONDS))
      import MyJsonProtocol._

      (keywords, _) =>
        val future = (levelDBActor ? LevelDBActor.GetLinks(keywords.toList))
          .mapTo[List[LevelDBActor.Item]]
          .map(_.toJson.compactPrint)

        onComplete(future) {
          case Success(value) =>
            logger.debug(s"Request succeeded: keywords = $keywords, response = $value")
            complete(HttpEntity(ContentTypes.`application/json`, value))
          case Failure(error) =>
            logger.warn(
              s"Request failed: keywords = $keywords, error = ${error.getMessage}"
            )
            complete(StatusCodes.InternalServerError, error.getMessage)
        }
    }

    val bindingFuture = Http().newServerAt("localhost", conf.port.apply()).bind(route)
    bindingFuture.onComplete {
      case Success(value) =>
        logger.info(s"Backend started at address ${value.localAddress.toString}")
      case Failure(exception) => throw exception
    }
  }

  private class Backend
}
