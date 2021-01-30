package me.sbarthol

import actors.DBActor
import akka.actor.{ActorSystem, Props}
import conf.ServerConf
import utils.{HBaseConnection, HttpServer}

import scala.concurrent.ExecutionContextExecutor

object Server {

  def main(args: Array[String]): Unit = {

    val conf = new ServerConf(args)
    implicit val system: ActorSystem = ActorSystem("Server")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val hbaseConn =
      HBaseConnection.init(
        zooKeeperAddress = conf.zooKeeperAddress(),
        zooKeeperPort = conf.zooKeeperPort()
      )

    sys.addShutdownHook {
      hbaseConn.close()
    }

    implicit val dbActor = system.actorOf(
      props = Props(
        new DBActor(hbaseConn)
      ),
      name = "db"
    )

    HttpServer.startServer(interface = conf.serverInterface(), port = conf.serverPort())
  }

  private class Server
}
