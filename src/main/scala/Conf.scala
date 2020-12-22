import org.rogach.scallop.{ScallopConf, ScallopOption}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

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
    validate = _.length >= 1,
    argName = "path"
  )

  val databaseDirectory: ScallopOption[String] = opt[String](
    name = "databaseDirectory",
    noshort = true,
    descr =
      "The directory in which to put the database files. Defaults to ./target.",
    default = Some("target"),
    validate = _.length >= 1,
    argName = "dir"
  )

  verify()
}
