import actors.LevelDBActor.Shutdown
import actors.{GetterActor, LevelDBActor, SchedulerActor}
import akka.actor.{ActorSystem, Props}
import client.NettyClient

object Main {

  def main(args: Array[String]): Unit = {

    // Todo: use the request/response pattern of Akka

    val system = ActorSystem("WebCrawler")

    val levelDBActor = system.actorOf(
      props = Props(
        new LevelDBActor(
          invertedIndexFilepath = "target/a",
          textFilepath = "target/b"
        )
      ),
      name = "levelDB"
    )

    val getterActor =
      system.actorOf(
        props = Props(
          new GetterActor(
            client = NettyClient.client,
            maxConcurrentConnections = 40
          )
        ),
        name = "getter"
      )
    val parserActor =
      system.actorOf(props = Props(new ParserActor), name = "parser")
    val seed = List()

    seed.foreach(source => {
      system.actorOf(
        props = Props(
          new SchedulerActor(
            source = source,
            maxDepth = 1,
            levelDBActor = levelDBActor,
            getterActor = getterActor
          )
        ),
        name = "scheduler"
      )
    })
  }
}
