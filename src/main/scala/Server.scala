import actors.DBActor
import akka.actor.{ActorSystem, Props}
import conf.ServerConf
import utils.HttpServer

import scala.concurrent.ExecutionContextExecutor

object Server {

  def main(args: Array[String]): Unit = {

    val conf = new ServerConf(args)
    implicit val system: ActorSystem = ActorSystem("Server")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    implicit val dbActor = system.actorOf(
      props = Props(
        new DBActor(
          zooKeeperAddress = conf.zooKeeperAddress(),
          zooKeeperPort = conf.zooKeeperPort()
        )
      ),
      name = "db"
    )

    HttpServer.startServer(port = conf.port())
  }

  private class Server
}
