package source
import scala.xml.XML

case class Source(link: String, depth: Int, crawlPresent: Boolean)

object SourcesLoader {

  def loadFromFile(filepath: String): List[Source] = {

    XML
      .loadFile(filepath)
      .\\("source")
      .toList
      .map(node =>
        Source(
          link = (node \ "link").text,
          depth = (node \ "depth").text.toInt,
          crawlPresent = (node \ "crawlPresent").text.toBoolean
        )
      )
  }
}
