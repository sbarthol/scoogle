import actors.LevelDBActor
import akka.actor.{ActorSystem, Props}
import conf.ServerConf
import utils.HttpServer

import scala.concurrent.ExecutionContextExecutor

object Server {

  def main(args: Array[String]): Unit = {

    val conf = new ServerConf(args)
    implicit val system: ActorSystem = ActorSystem("Server")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val levelDBActor = system.actorOf(
      props = Props(
        new LevelDBActor(
          databaseDirectory = conf.databaseDirectory.apply()
        )
      ),
      name = "levelDB"
    )

    HttpServer.createRoutes(levelDBActor = levelDBActor, port = conf.port.apply())
  }

  private class Server
}
