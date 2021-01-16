package utils

import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.{CompareOperator, HBaseConfiguration, TableName}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.SeqHasAsJava

object HBaseConnection {

  private val logger = LoggerFactory.getLogger(classOf[HBaseConnection])

  def getConnection(zooKeeperAddress: String, zooKeeperPort: Int): HBaseConnection = {

    val config = HBaseConfiguration.create
    config.set("hbase.zookeeper.quorum", zooKeeperAddress)
    config.setInt("hbase.zookeeper.property.clientPort", zooKeeperPort)

    HBaseAdmin.available(config)
    logger.debug(s"HBase master is available")

    val connection = ConnectionFactory.createConnection(config)
    val admin = connection.getAdmin
    new HBaseConnection(connection = connection, admin = admin)
  }
}

class HBaseConnection private (connection: Connection, admin: Admin) {

  def init(): Unit = {

    createTable(
      name = "website",
      families = List("text", "metadata")
    )

    createTable(
      name = "invertedIndex",
      families = List("index")
    )

    createTable(
      name = "blacklist",
      families = List("link")
    )
  }

  private def createTable(name: String, families: List[String]): Unit = {

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
  }

  def close(): Unit = {
    connection.close()
  }

  def isBlacklisted(link: String): Boolean = {

    val blacklistTable = connection.getTable(TableName.valueOf("blacklist"))
    val filter = new SingleColumnValueFilter(
      "link".getBytes,
      Array.empty,
      CompareOperator.EQUAL,
      link.getBytes
    )

    val scan = new Scan()
    scan.setFilter(filter)

    val scanner = blacklistTable.getScanner(scan)
    scanner.iterator().hasNext
  }

  def isInDb(link: String): Boolean = {

    val blacklistTable = connection.getTable(TableName.valueOf("websites"))
    val filter = new SingleColumnValueFilter(
      "metadata".getBytes,
      "link".getBytes,
      CompareOperator.EQUAL,
      link.getBytes
    )

    val scan = new Scan()
    scan.setFilter(filter)

    val scanner = blacklistTable.getScanner(scan)
    scanner.iterator().hasNext
  }

  def blacklist(link: String): Unit = {


  }
}
