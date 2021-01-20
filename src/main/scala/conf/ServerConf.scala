package conf

import org.rogach.scallop.ScallopConf

class ServerConf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner(
    """This program is a server that answers
      | hosts the static webpage and answers
      | requests to the prefilled HBase store""".stripMargin
  )

  val port = opt[Int](
    name = "port",
    noshort = true,
    descr = "The port number on which the server listens.",
    required = true,
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "port"
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

  verify()
}
