package utils

import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.SeqHasAsJava

object HBaseConnection {

  private val logger = LoggerFactory.getLogger(classOf[HBaseConnection])

  def init(zooKeeperAddress: String, zooKeeperPort: Int): HBaseConnection = {

    val config = HBaseConfiguration.create
    config.set("hbase.zookeeper.quorum", zooKeeperAddress)
    config.setInt("hbase.zookeeper.property.clientPort", zooKeeperPort)

    HBaseAdmin.available(config)
    logger.debug(s"HBase master is available")

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

    val blacklistTable = createTable(
      name = "blacklist",
      families = List("blacklisted")
    )

    new HBaseConnection(
      connection = connection,
      invertedIndexTable = invertedIndexTable,
      blacklistTable = blacklistTable,
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

      HBaseConnection.logger.debug(
        s"Created table $name with families ${families.toString}"
      )
    } else {
      HBaseConnection.logger.debug(s"Table $name already exists")
    }

    connection.getTable(tableName)
  }
}

class HBaseConnection private (
    connection: Connection,
    invertedIndexTable: Table,
    blacklistTable: Table,
    websitesTable: Table
) {

  def close(): Unit = {
    connection.close()
  }

  def isBlacklisted(link: String): Boolean = {

    val get = new Get(link.getBytes)
    get.addColumn("blacklisted".getBytes, "link".getBytes)
    blacklistTable.exists(get)
  }

  def isInDb(link: String): Boolean = {

    val get = new Get(link.getBytes)
    get.addColumn("metadata".getBytes, "link".getBytes)
    websitesTable.exists(get)
  }

  def blacklist(link: String): Unit = {

    val put = new Put(link.getBytes)
    put.addColumn("blacklisted".getBytes, "link".getBytes, Array.empty)
    blacklistTable.put(put)
  }

  def putWebsite(link: String, text: String, title: String): Unit = {

    val put = new Put(link.getBytes)
    put.addColumn("metadata".getBytes, "title".getBytes, title.getBytes)
    put.addColumn("content".getBytes, "text".getBytes, text.getBytes)
    websitesTable.put(put)
  }

  def putWords(link: String, words: List[(String, Int)]): Unit = {

    invertedIndexTable.put(words.map {
      case (word, count) =>
        val put = new Put(link.getBytes)
        put.addColumn("index".getBytes, "word".getBytes, word.getBytes)
        put.addColumn("index".getBytes, "count".getBytes, count.toHexString.getBytes)
        put
    }.asJava)
  }
}
