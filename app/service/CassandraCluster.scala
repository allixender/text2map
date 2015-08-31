package service

import com.datastax.driver.core.{ ProtocolOptions, Cluster }
import play.api.Play.current
import java.util.{List, ArrayList}

trait ConfigCassandraCluster {

  import scala.collection.JavaConversions._
  private val cassandraConfig = current.configuration.getConfig("cassandra.main.db.cassandra")
  private val port = cassandraConfig.get.getInt("port").get
  private val hosts = cassandraConfig.get.getStringList("hosts").get

  lazy val cluster: Cluster =
    Cluster.builder().
      addContactPoints(hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(port).
      build()
}

object CFKeys {
  private val cassandraConfig = current.configuration.getConfig("cassandra.main.db.cassandra")
  val playCassandra = cassandraConfig.get.getString("keyspace").get
  val articles = "articles"
  val linzgeo = "linzgeo"
  val geomatch = "geomatch"
  val metaxml = "metaxml"
}
