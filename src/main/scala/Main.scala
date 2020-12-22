import actors.{GetterActor, LevelDBActor, SchedulerActor}
import akka.actor.{ActorSystem, Props}
import client.NettyClient

object Main {

  def main(args: Array[String]): Unit = {

    val conf = new Conf(args)
    val system = ActorSystem("WebCrawler")

    val levelDBActor = system.actorOf(
      props = Props(
        new LevelDBActor(
          invertedIndexFilepath = conf.databaseDirectory.apply() + "/invertedIndexDb",
          textFilepath = conf.databaseDirectory.apply() + "/textDb"
        )
      ),
      name = "levelDB"
    )

    val getterActor =
      system.actorOf(
        props = Props(
          new GetterActor(
            client = NettyClient.client,
            maxConcurrentConnections = conf.maxConcurrentSockets.apply()
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
