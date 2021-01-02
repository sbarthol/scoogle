import actors.{GetterActor, ParserActor, SchedulerActor}
import akka.actor.{Actor, ActorSystem, Props}
import client.NettyClient
import org.slf4j.LoggerFactory

object Main {

  def main(args: Array[String]): Unit = {

    // Todo: use the request/response pattern of Akka

    val system = ActorSystem("WebCrawler")

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
            getterActor = getterActor,
            parserActor = parserActor
          )
        ),
        name = "scheduler"
      )
    })

    system.actorOf(
      Props(new Actor {

        private val logger = LoggerFactory.getLogger("dummy")

        while (true) {
          Thread.sleep(1000 * 3)
          logger.info(s"Sending the words")
          parserActor ! ParserActor.Find(List("cia","nick","csi"))
        }

        override def receive: Receive = { case ParserActor.Result(links) =>

          logger.info(s"The links are ${links}")
        }
      }),
      name = "dummy"
    )
  }
}
