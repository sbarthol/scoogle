import actors.MasterActor
import akka.actor.{ActorSystem, Props}
import conf.Conf
import source.SourcesLoader

object WebScraper {

  def main(args: Array[String]): Unit = {

    val conf = new Conf(args)
    val sources = SourcesLoader.loadFromFile(filepath = conf.sourceFilepath.apply())
    val system = ActorSystem("WebCrawler")

    system.actorOf(
      props = Props(
        new MasterActor(
          sources = sources,
          databaseDirectory = conf.databaseDirectory.apply(),
          maxConcurrentSockets = conf.maxConcurrentSockets.apply(),
          overridePresentLinks = conf.overridePresentLinks.toOption match {
            case Some(true) => true
            case _          => false
          }
        )
      ),
      name = "master"
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
