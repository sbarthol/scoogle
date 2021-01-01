package conf

import org.rogach.scallop.{ScallopConf, ScallopOption}

class ServerConf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner(
    """This program is a server that answers
      | hosts the static webpage and answers
      | requests to the prefilled levelDB database""".stripMargin
  )

  val port: ScallopOption[Int] = opt[Int](
    name = "port",
    noshort = true,
    descr = "The port number on which the server listens.",
    required = true,
    validate = p => { p >= 0 && p <= (1 << 16) },
    argName = "portNumber"
  )

  val databaseDirectory: ScallopOption[String] = opt[String](
    name = "databaseDirectory",
    noshort = true,
    descr = "The directory containing the database files. Defaults to ./target.",
    default = Some("target"),
    validate = _.nonEmpty,
    argName = "dir"
  )

  verify()
}
