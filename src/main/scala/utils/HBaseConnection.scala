package utils

import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.slf4j.LoggerFactory
import utils.HBaseConnection.log

import scala.collection.mutable
import scala.jdk.CollectionConverters.{IteratorHasAsScala, SeqHasAsJava}

object HBaseConnection {

  private val log = LoggerFactory.getLogger(classOf[HBaseConnection])

  def init(zooKeeperAddress: String, zooKeeperPort: Int): HBaseConnection = {

    val config = HBaseConfiguration.create
    config.set("hbase.zookeeper.quorum", zooKeeperAddress)
    config.setInt("hbase.zookeeper.property.clientPort", zooKeeperPort)

    implicit val connection = ConnectionFactory.createConnection(config)
    implicit val admin = connection.getAdmin

    val websitesTable = createTable(
      name = "websites",
      families = List("content", "metadata")
    )

    val invertedIndexTable = createTable(
      name = "invertedIndex",
      families = List("index")
    )

    new HBaseConnection(
      connection = connection,
      invertedIndexTable = invertedIndexTable,
      websitesTable = websitesTable
    )
  }

  private def createTable(name: String, families: List[String])(implicit
      admin: Admin,
      connection: Connection
  ): Table = {

    val tableName = TableName.valueOf(name)
    if (!admin.tableExists(tableName)) {

      val familyDescriptors =
        families.map(f => ColumnFamilyDescriptorBuilder.newBuilder(f.getBytes).build())

      val table =
        TableDescriptorBuilder
          .newBuilder(tableName)
          .setColumnFamilies(familyDescriptors.asJava)
          .build()
      admin.createTable(table)

      HBaseConnection.log.info(
        s"Created table $name with families ${families.toString}"
      )
    } else {
      HBaseConnection.log.info(s"Table $name already exists")
    }

    connection.getTable(tableName)
  }
}

class HBaseConnection private (
    connection: Connection,
    invertedIndexTable: Table,
    websitesTable: Table
) {

  def close(): Unit = {
    connection.close()
  }

  def getWebsite(hash: String): (String, String, String) = {

    val getTitle = new Get(hash.getBytes)
    getTitle.addColumn("metadata".getBytes, "title".getBytes)
    val title = new String(
      CellUtil.cloneValue(websitesTable.get(getTitle).rawCells().head)
    )

    val getText = new Get(hash.getBytes)
    getText.addColumn("content".getBytes, "text".getBytes)
    val text = new String(CellUtil.cloneValue(websitesTable.get(getText).rawCells().head))

    val getLink = new Get(hash.getBytes)
    getLink.addColumn("metadata".getBytes, "link".getBytes)
    val link = new String(CellUtil.cloneValue(websitesTable.get(getLink).rawCells().head))

    (title, text, link)
  }

  def putWebsite(hash: String, link: String, text: String, title: String): Unit = {

    val put = new Put(hash.getBytes)
    put.addColumn("metadata".getBytes, "title".getBytes, title.getBytes)
    put.addColumn("content".getBytes, "text".getBytes, text.getBytes)
    put.addColumn("metadata".getBytes, "link".getBytes, link.getBytes)
    websitesTable.put(put)
  }

  def putWords(hash: String, words: List[(String, Int)]): Unit = {

    invertedIndexTable.put(words.map { case (word, count) =>
      val put = new Put(s"${word}_$hash".getBytes)
      put.addColumn("index".getBytes, "count".getBytes, count.toHexString.getBytes)
      put
    }.asJava)
  }

  def getHashes(words: List[String]): List[(String, Int)] = {

    // Todo: even better create a DB with word -> number of websites containing that word
    val descendingOrder = words.sortBy(-_.length)
    val longestWord = descendingOrder.head
    log.debug(s"Longest word = $longestWord")

    val matchedLinks = mutable.Map[String, Int]()

    val scan = new Scan()
    scan.setRowPrefixFilter((longestWord + "_").getBytes)
    val rows = invertedIndexTable.getScanner(scan).iterator.asScala.toList

    log.debug(
      s"""Found ${rows.size} links
         |for request longest word = $longestWord""".stripMargin
    )

    rows.foreach(row => {

      val List(_, hash) = new String(row.getRow).split("_").toList
      val count =
        Integer.valueOf(new String(CellUtil.cloneValue(row.rawCells().head)), 16).toInt

      matchedLinks.put(key = hash, value = count)
    })

    descendingOrder.tail.foreach(word => {

      val hashes = matchedLinks.keys.toList

      val gets = hashes
        .map(hash => {

          val row = word + "_" + hash
          val get = new Get(row.getBytes)
          get.addColumn("index".getBytes, "count".getBytes)
          get
        })

      val results = invertedIndexTable.get(gets.asJava)

      results.zip(hashes).foreach { case (result, hash) =>
        if (result.isEmpty) {
          matchedLinks.remove(hash)
        } else {

          val count = Integer
            .valueOf(new String(CellUtil.cloneValue(result.rawCells().head)), 16)
            .toInt
          matchedLinks.put(key = hash, value = math.min(matchedLinks(hash), count))
        }
      }
    })

    matchedLinks.toList.sortBy { case (_, count) => -count }
  }
}
