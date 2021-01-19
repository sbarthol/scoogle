package utils

import actors.DBActor
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Server
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory
import spray.json.enrichAny
import utils.JsonProtocol.responseFormat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success}

object HttpServer {

  private val log = LoggerFactory.getLogger(classOf[HttpServer])

  def startServer(port: Int)(implicit
      ec: ExecutionContext,
      system: ActorSystem,
      dbActor: ActorRef
  ): Unit = {

    val apiRoute = getApiRoute(dbActor)
    val frontendRoute = getFrontendRoute

    val bindingFuture =
      Http().newServerAt("localhost", port).bind(apiRoute ~ frontendRoute)

    bindingFuture.onComplete {
      case Success(value) =>
        log.info(s"Server started at address ${value.localAddress.toString}")
      case Failure(exception) => throw exception
    }
  }

  private def getFrontendRoute: Route = {

    getFromDirectory(Server.getClass.getResource("/build").getPath) ~ path("") {
      getFromFile(
        Server.getClass.getResource("/build/index.html").getFile
      )
    }
  }

  private def getApiRoute(
      dbActor: ActorRef
  )(implicit ec: ExecutionContext): Route = {

    path("api") {
      get {
        parameters("query", "pageNumber".as[Int]) {

          implicit val timeout: Timeout =
            Timeout(FiniteDuration(length = 20, unit = SECONDS))

          (query, pageNumber) => {

            val keywords = query.trim.split("\\s+").toList.map(_.toLowerCase).distinct
            val future = (dbActor ? DBActor.GetLinks(keywords, pageNumber))
              .mapTo[DBActor.Response]
              .map(_.toJson.compactPrint)

            onComplete(future) {
              case Success(value) =>
                log.debug(
                  s"Request succeeded: keywords = $keywords"
                )
                complete(HttpEntity(ContentTypes.`application/json`, value))
              case Failure(error) =>
                log.warn(
                  s"Request failed: keywords = $keywords, error = ${error.getMessage}"
                )
                complete(StatusCodes.InternalServerError, error.getMessage)
            }
          }
        }
      }
    }
  }

  private class HttpServer
}
