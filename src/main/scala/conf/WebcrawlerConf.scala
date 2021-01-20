package conf

import org.rogach.scallop.ScallopConf

class WebcrawlerConf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""This program is a web crawler that takes as input list of links as a
      |starting point. It then downloads the links and stores the contents
      |as well as an inverted index in a HBase store.
      |""".stripMargin)

  val maxConcurrentSockets = opt[Int](
    name = "maxConcurrentSockets",
    noshort = true,
    descr =
      "The maximum number of sockets that the program will open simultaneously. Defaults to 30.",
    default = Some(30),
    validate = _ >= 1,
    argName = "max"
  )

  val zooKeeperAddress = opt[String](
    name = "zooKeeperAddress",
    noshort = true,
    descr = "The address of the ZooKeeper Quorum server. Defaults 'localhost'",
    default = Some("localhost"),
    validate = _.nonEmpty,
    argName = "addr"
  )

  val zooKeeperPort = opt[Int](
    name = "zooKeeperPort",
    noshort = true,
    descr = "The port of the ZooKeeper Quorum server. Defaults '2181'",
    default = Some(2181),
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "port"
  )

  val sourceFilepaths = trailArg[List[String]](
    name = "sourceFilepaths",
    descr = "The list of filepaths of the sources containing the source links.",
    validate = l => { l.nonEmpty && l.forall(_.nonEmpty) },
    required = true
  )

  verify()
}
