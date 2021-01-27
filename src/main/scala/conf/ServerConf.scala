package conf

import org.rogach.scallop.ScallopConf

class ServerConf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner(
    """This program is a server that answers
      | hosts the static webpage and answers
      | requests to the prefilled HBase store""".stripMargin
  )

  val serverInterface = opt[String](
    name = "serverInterface",
    noshort = true,
    descr = "The interface on which the server listens. Defaults to 'localhost'.",
    default = Some("localhost"),
    validate = _.nonEmpty,
    argName = "serverInterface"
  )

  val serverPort = opt[Int](
    name = "serverPort",
    noshort = true,
    descr = "The port number on which the server listens. Defaults to 8080.",
    default = Some(8080),
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "serverPort"
  )

  val zooKeeperAddress = opt[String](
    name = "zooKeeperAddress",
    noshort = true,
    descr = "The address of the ZooKeeper Quorum server. Defaults to 'localhost'.",
    default = Some("localhost"),
    validate = _.nonEmpty,
    argName = "addr"
  )

  val zooKeeperPort = opt[Int](
    name = "zooKeeperPort",
    noshort = true,
    descr = "The port of the ZooKeeper Quorum server. Defaults to 2181.",
    default = Some(2181),
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "port"
  )

  verify()
}
