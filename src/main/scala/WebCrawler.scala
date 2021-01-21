import actors.MasterActor
import akka.actor.{ActorSystem, Props}
import conf.WebCrawlerConf
import kamon.Kamon
import source.SourcesLoader

object WebCrawler {

  def main(args: Array[String]): Unit = {

    Kamon.init()

    val conf = new WebCrawlerConf(args)
    val sources = SourcesLoader.loadFromFiles(filepaths = conf.sourceFilepaths())
    val system = ActorSystem("WebCrawler")

    system.actorOf(
      props = Props(
        new MasterActor(
          sources = sources,
          zooKeeperAddress = conf.zooKeeperAddress(),
          zooKeeperPort = conf.zooKeeperPort(),
          maxConcurrentSockets = conf.maxConcurrentSockets()
        )
      ),
      name = "master"
    )
  }
}
