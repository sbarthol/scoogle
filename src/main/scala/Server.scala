import actors.DBActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import conf.ServerConf
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val itemFormat: RootJsonFormat[DBActor.Item] = jsonFormat4(
    DBActor.Item
  )
  implicit val responseFormat: RootJsonFormat[DBActor.Response] = jsonFormat2(
    DBActor.Response
  )
}

object Server {

  private val logger = LoggerFactory.getLogger(classOf[Server])

  def main(args: Array[String]): Unit = {

    val conf = new ServerConf(args)
    implicit val system: ActorSystem = ActorSystem("Server")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val dbActor = system.actorOf(
      props = Props(
        new DBActor(
          zooKeeperAddress = conf.zooKeeperAddress.apply(),
          zooKeeperPort = conf.zooKeeperPort.apply()
        )
      ),
      name = "db"
    )

    val frontendRoute =
      getFromDirectory(Server.getClass.getResource("/build").getPath) ~ path("") {
        getFromFile(
          Server.getClass.getResource("/build/index.html").getFile
        )
      }

    val apiRoute = path("api") {
      get {
        parameters("query", "pageNumber".as[Int]) {

          implicit val timeout: Timeout =
            Timeout(FiniteDuration(length = 20, unit = SECONDS))
          import MyJsonProtocol._

          (query, pageNumber) => {

            val keywords = query.split(" ").toList
            val future = (dbActor ? DBActor.GetLinks(keywords, pageNumber))
              .mapTo[DBActor.Response]
              .map(_.toJson.compactPrint)

            onComplete(future) {
              case Success(value) =>
                logger.debug(
                  s"Request succeeded: keywords = $keywords, response = $value"
                )
                complete(HttpEntity(ContentTypes.`application/json`, value))
              case Failure(error) =>
                logger.warn(
                  s"Request failed: keywords = $keywords, error = ${error.getMessage}"
                )
                complete(StatusCodes.InternalServerError, error.getMessage)
            }
          }
        }
      }
    }

    val bindingFuture =
      Http().newServerAt("localhost", conf.port.apply()).bind(apiRoute ~ frontendRoute)

    bindingFuture.onComplete {
      case Success(value) =>
        logger.info(s"Server started at address ${value.localAddress.toString}")
      case Failure(exception) => throw exception
    }
  }

  private class Server
}
