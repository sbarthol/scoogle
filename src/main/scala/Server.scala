import actors.{FindActor, LevelDBActor}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success}

object Server {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("Server")
    implicit val ec = system.dispatcher

    val findActor = system.actorOf(
      props = Props(
        new FindActor(
          databaseDirectory =  "target", // Todo: parse from argument
        )
      ),
      name = "find"
    )

    implicit val timeout: Timeout =
      Timeout(duration = FiniteDuration(5, SECONDS))
    val linksFuture = findActor ? FindActor.Find(words = List())
    linksFuture.onComplete {
      case Success(value) => ???
      case Failure(exception) => throw exception
    }

    val route =
      path("hello") {
        get {
          complete(
            HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>")
          )
        }
      }
    1
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    bindingFuture.onComplete {
      case Success(_)         => ???
      case Failure(exception) => throw exception
    }
  }
}
