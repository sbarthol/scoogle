package source
import scala.xml.XML

case class Source(link: String, depth: Int)

object SourcesLoader {

  def loadFromFiles(filepaths: List[String]): List[Source] = {

    filepaths.flatMap(loadFromFile)
  }

  private def loadFromFile(filepath: String): List[Source] = {

    XML
      .loadFile(filepath)
      .\\("source")
      .toList
      .map(node =>
        Source(
          link = (node \ "link").text,
          depth = (node \ "depth").text.toInt
        )
      )
  }
}
