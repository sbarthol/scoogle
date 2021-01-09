import actors.MasterActor
import akka.actor.{ActorSystem, Props}
import conf.WebCrawlerConf
import kamon.Kamon
import source.SourcesLoader

object WebCrawler {

  def main(args: Array[String]): Unit = {

    Kamon.init()

    val conf = new WebCrawlerConf(args)
    val sources = SourcesLoader.loadFromFile(filepath = conf.sourceFilepath.apply())
    val system = ActorSystem("WebCrawler")

    system.actorOf(
      props = Props(
        new MasterActor(
          sources = sources,
          databaseDirectory = conf.databaseDirectory.apply(),
          maxConcurrentSockets = conf.maxConcurrentSockets.apply()
        )
      ),
      name = "master"
    )
  }
}
