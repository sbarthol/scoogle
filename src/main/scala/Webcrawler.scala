import actors.MasterActor
import akka.actor.{ActorSystem, Props}
import conf.WebcrawlerConf
import kamon.Kamon
import source.SourcesLoader

object Webcrawler {

  def main(args: Array[String]): Unit = {

    Kamon.init()

    val conf = new WebcrawlerConf(args)
    val sources = SourcesLoader.loadFromFile(filepath = conf.sourceFilepath.apply())
    val system = ActorSystem("Webcrawler")

    system.actorOf(
      props = Props(
        new MasterActor(
          sources = sources,
          zooKeeperAddress = conf.zooKeeperAddress.apply(),
          zooKeeperPort = conf.zooKeeperPort.apply(),
          maxConcurrentSockets = conf.maxConcurrentSockets.apply()
        )
      ),
      name = "master"
    )
  }
}
