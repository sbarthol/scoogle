package conf

import org.rogach.scallop.{ScallopConf, ScallopOption}

class WebCrawlerConf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""This program is a webcrawler that takes as input list of links as a
      |starting point. It then downloads the links and stores the contents
      |as well as an inverted index in a HBase store.
      |""".stripMargin)

  val maxConcurrentSockets: ScallopOption[Int] = opt[Int](
    name = "maxConcurrentSockets",
    noshort = true,
    descr =
      "The maximum number of sockets that the program will open simultaneously. Defaults to 30.",
    default = Some(30),
    validate = _ >= 1,
    argName = "max"
  )

  val sourceFilepath: ScallopOption[String] = opt[String](
    name = "sourceFilepath",
    noshort = true,
    descr = "The filepath of the source file containing the source links.",
    required = true,
    validate = _.nonEmpty,
    argName = "path"
  )

  val zooKeeperAddress: ScallopOption[String] = opt[String](
    name = "zooKeeperAddress",
    noshort = true,
    descr = "The address of the ZooKeeper Quorum server. Defaults 'localhost'",
    default = Some("localhost"),
    validate = _.nonEmpty,
    argName = "addr"
  )

  val zooKeeperPort: ScallopOption[Int] = opt[Int](
    name = "zooKeeperPort",
    noshort = true,
    descr = "The port of the ZooKeeper Quorum server. Defaults '2181'",
    default = Some(2181),
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "port"
  )

  verify()
}
