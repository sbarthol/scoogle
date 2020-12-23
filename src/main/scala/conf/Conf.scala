package conf

import org.rogach.scallop.{ScallopConf, ScallopOption}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""This program is a webscraper that takes as input list of links as a
      |starting point. It then downloads the links and stores the contents
      |as well as an inverted index in a LevelDB database.
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

  val databaseDirectory: ScallopOption[String] = opt[String](
    name = "databaseDirectory",
    noshort = true,
    descr = "The directory in which to put the database files. Defaults to ./target.",
    default = Some("target"),
    validate = _.nonEmpty,
    argName = "dir"
  )

  val overridePresentLinks: ScallopOption[Boolean] = toggle(
    name = "overridePresentLinks",
    noshort = true,
    descrYes =
      "Redownloads links already present in the database and overrides the content",
    descrNo = "Does not redownload the links already present in the database"
  )

  verify()
}
