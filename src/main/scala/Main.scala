import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import org.slf4j.LoggerFactory

class Greeter extends Actor {

  val logger = LoggerFactory.getLogger("oklm")
  override def receive: Receive = { case s: String =>
    logger.error(s)
  }
}

object Main {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("pingpong")
    val greeter = system.actorOf(props = Props[Greeter], name = "Sacha")
    greeter ! (message = "coucou")
    Thread.sleep(500)
    val logger = LoggerFactory.getLogger("chapters.introduction.HelloWorld1")
    logger.debug("Hello world.")
    //greeter ! PoisonPill
    system.terminate()
  }
}
