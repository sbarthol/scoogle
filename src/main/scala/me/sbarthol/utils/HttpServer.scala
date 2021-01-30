package me.sbarthol.utils

import me.sbarthol.actors.DBActor
import me.sbarthol.actors.ParserActor.extractWords
import akka.actor.{ActorRef, ActorSystem}
import akka.http.javadsl.server.PathMatchers.remaining
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory
import spray.json.enrichAny
import me.sbarthol.utils.JsonProtocol.responseFormat

import java.net.URLDecoder
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

object HttpServer {

  private val log = LoggerFactory.getLogger(classOf[HttpServer])

  def startServer(interface: String, port: Int)(implicit
      ec: ExecutionContext,
      system: ActorSystem,
      dbActor: ActorRef
  ): Unit = {

    val apiRoute = getApiRoute(dbActor)
    val frontendRoute = getFrontendRoute
    val fileRoute = getFileRoute

    val bindingFuture =
      Http().newServerAt(interface, port).bind(apiRoute ~ frontendRoute ~ fileRoute)

    bindingFuture.onComplete {
      case Success(value) =>
        log.info(s"Server started at address ${value.localAddress.toString}")
      case Failure(exception) => throw exception
    }
  }

  private def getFrontendRoute: Route = {

    getFromResourceDirectory("build") ~ path("") {
      getFromResource("build/index.html")
    }
  }

  private def getFileRoute(implicit ec: ExecutionContext): Route = {

    path(remaining.toScala) { path =>
      get {

        val future = Future {
          val source =
            Source.fromFile("/" + URLDecoder.decode(path, "UTF-8"), "UTF-8")
          val content = source.mkString
          source.close
          content
        }

        onComplete(future) {

          case Success(html) =>
            log.debug(s"Request succeeded: path = $path")
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))

          case Failure(error) =>
            log.warn(
              s"Request failed: path = $path, error = ${error.getMessage}"
            )
            complete(StatusCodes.InternalServerError, error.getMessage)
        }
      }
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

            val keywords = extractWords(query).distinct

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
