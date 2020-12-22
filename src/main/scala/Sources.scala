import scala.xml.XML

case class Source(link: String, depth: Int)

object SourcesLoader {

  def loadFromFile(filepath: String): List[Source] = {

    XML
      .loadFile(filepath)
      .\\("source")
      .toList
      .map(node =>
        Source(link = (node \ "link").text, depth = (node \ "depth").text.toInt)
      )
  }
}
