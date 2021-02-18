package me.sbarthol.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import me.sbarthol.actors.ParserActor.{Body, toKeywords}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.{
  LetterTokenizerFactory,
  LowerCaseFilterFactory,
  StopFilterFactory
}
import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.en.PorterStemFilterFactory
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory
import org.apache.lucene.analysis.standard.{StandardAnalyzer, StandardTokenizerFactory}
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import java.net.URL
import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.control.Breaks.{break, breakable}

class ParserActor(dbActorManager: ActorRef) extends Actor with ActorLogging {

  private val maximumTextLength = 500000

  override def receive: Receive = { case Body(link, html, schedulerActor) =>
    schedulerActor ! SchedulerActor.NewLinks(link, getLinks(html, link))

    val text = getText(html)
    val title = getTitle(html)
    val words = getWords(title = title, text = text, host = getHost(link))

    log.debug(s"Link $link contains title $title text $text")

    if (words.nonEmpty) {

      dbActorManager ! DBActor.Put(
        words = words,
        link = link,
        text = text,
        title = title
      )
    }
  }

  private def getTitle(html: String): String = {

    val doc = Jsoup.parse(html)

    def getHeader(header: String): String = {
      doc.select(header) match {
        case elements if !elements.isEmpty => elements.get(0).text()
        case _                             => ""
      }
    }

    val titles =
      List(doc.title(), getHeader("h1"), getHeader("h2"), getHeader("h3"))
        .filterNot(_.isEmpty)
        .distinct

    titles.foldLeft(z = "") {
      case (pref, t) if t.nonEmpty && pref.nonEmpty => pref + " " + t
      case (_, t) if t.nonEmpty                     => t
    } match {
      case t if t.isEmpty => "No Title Found"
      case t              => t
    }
  }

  private def getText(html: String): String = {

    try {

      val relevantTags =
        HashSet[String]() ++ List("p", "h1", "h2", "h3", "pre", "blockquote")

      def dfs(el: Element, insideRelevant: Boolean): String = {

        var currentElementText =
          if (insideRelevant) el.ownText().strip()
          else ""

        val it = el.children().iterator()
        while (it.hasNext) {
          val child = it.next()

          dfs(
            child,
            insideRelevant || relevantTags.contains(child.tagName())
          ) match {
            case s if s.nonEmpty && currentElementText.nonEmpty =>
              currentElementText = currentElementText + " " + s
            case s if s.nonEmpty => currentElementText = s
            case _ =>
          }
        }
        currentElementText
      }

      dfs(el = Jsoup.parse(html).body(), insideRelevant = false).take(maximumTextLength)

    } catch {

      case e: Exception =>
        log.error(s"Could not parse text: ${e.toString}")
        ""
    }
  }

  private def getWords(
      title: String,
      host: String,
      text: String
  ): List[(String, Int)] = {

    val titleWords = toKeywords(title)
      .groupBy(identity)
      .view
      .mapValues(_.size * 1000)
      .toList

    val hostWords = toKeywords(host)
      .groupBy(identity)
      .view
      .mapValues(_.size * 1000)
      .toList

    val textWords = toKeywords(text)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toList

    (titleWords ++ textWords ++ hostWords).groupMapReduce { case (word, _) =>
      word
    } { case (_, count) =>
      count
    }(_ + _).toList
  }

  private def getHost(link: String): String = {
    new URL(link).getHost
  }

  private def getLinks(html: String, link: String): List[String] = {

    Jsoup
      .parse(html, link)
      .select("a")
      .asScala
      .map(_.absUrl("href"))
      .filter(_.nonEmpty)
      .toList
      .distinct
  }
}

object ParserActor {

  private lazy val keywordAnalyser =
    CustomAnalyzer
      .builder()
      .withTokenizer(LetterTokenizerFactory.NAME)
      .addTokenFilter(LengthFilterFactory.NAME, "min", "3", "max", "20")
      .addTokenFilter(LowerCaseFilterFactory.NAME)
      .addTokenFilter(StopFilterFactory.NAME)
      .addTokenFilter(PorterStemFilterFactory.NAME)
      .addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "synonyms2.txt")
      .build()

  private lazy val synonymAnalyser =
    CustomAnalyzer
      .builder()
      .withTokenizer(StandardTokenizerFactory.NAME)
      .addTokenFilter(LowerCaseFilterFactory.NAME)
      .addTokenFilter(PorterStemFilterFactory.NAME)
      .addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "synonyms2.txt")
      .build()

  private def tokenize(text: String, analyzer: Analyzer): List[String] = {

    val tokenStream = analyzer.tokenStream("", text)
    tokenStream.reset()

    val wordList = ListBuffer[String]()
    while (tokenStream.incrementToken()) {
      val token = tokenStream.getAttribute(classOf[CharTermAttribute]).toString
      wordList.addOne(token)
    }
    tokenStream.close()
    wordList.toList
  }

  def toKeywords(text: String): List[String] = {
    tokenize(text, keywordAnalyser).flatMap(_.split(" ")).filter(_.length > 1)
  }

  private def toWords(text: String): List[String] = {
    tokenize(text, new StandardAnalyzer)
  }

  private def toSynonyms(text: String): List[String] = {
    tokenize(text, synonymAnalyser).flatMap(_.split(" "))
  }

  def highlight(text: String, keywords: List[String]): String = {

    val numberWrappingWords = 4
    val maxTextLength = 2000
    val sb = new StringBuilder()
    val keywordSet = HashSet[String]() ++ keywords

    def addToSb(w: String, bold: Boolean): Unit = {
      if (bold) sb.addAll("<strong>")
      sb.addAll(w)
      if (bold) sb.addAll("</strong>")
    }

    val words = toWords(text).toArray
    val synonyms = toSynonyms(text).toArray
    assert(words.length == synonyms.length)

    var i = 0
    breakable {
      while (i < words.length) {

        if (sb.size >= maxTextLength) break
        if (
          i + 1 < words.length && synonyms(i) == "_" && keywordSet.contains(
            synonyms(i + 1)
          )
        ) {

          val start = math.max(0, i - numberWrappingWords)
          val end = math.min(i + numberWrappingWords, words.length - 1)

          for (j <- start until end) {
            addToSb(words(j), i == j || i + 1 == j)
            sb.addOne(' ')
          }
          addToSb(words(end), i + 1 == end)
          sb.addAll("... ")
          i = i + 2
        } else if (keywordSet.contains(synonyms(i))) {

          val start = math.max(0, i - numberWrappingWords)
          val end = math.min(i + numberWrappingWords, words.length - 1)

          for (j <- start until end) {
            addToSb(words(j), i == j)
            sb.addOne(' ')
          }
          addToSb(words(end), i == end)
          sb.addAll("... ")
          i = i + 1
        } else {
          i = i + 1
        }
      }
    }
    sb.toString
  }

  case class Body(link: String, html: String, schedulerActor: ActorRef)
}
