import actors.{GetterActor, LevelDBActor, SchedulerActor}
import akka.actor.{ActorSystem, Props}
import client.NettyClient
import org.slf4j.LoggerFactory

object Main {

  private class Main
  private val logger = LoggerFactory.getLogger(classOf[Main])

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

    sources.foreach(source => {
      system.actorOf(
        props = Props(
          new SchedulerActor(
            source = source.link,
            maxDepth = source.depth,
            levelDBActor = levelDBActor,
            getterActor = getterActor
          )
        )
      )
    })
  }
}
