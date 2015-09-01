package models

import com.datastax.driver.core.DataType._
import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => ceq}
import com.datastax.driver.core.schemabuilder.SchemaBuilder

import service.{CFKeys, ConfigCassandraCluster}
import java.util.{List, ArrayList}

import scala.concurrent.Future

case class GeoMatch(
                     articleid: Long,
                     titlematch: scala.IndexedSeq[Long],
                     abstractmatch: scala.IndexedSeq[Long],
                     fulltextmatch: scala.IndexedSeq[Long]
                     )

object GeoMatch extends ConfigCassandraCluster {

  import cassandra.resultset._
  import play.api.libs.concurrent.Execution.Implicits._

  import scala.collection.JavaConversions._
  import scala.language.implicitConversions

  lazy val session = cluster.connect(CFKeys.playCassandra)

  val keysClass = com.google.common.reflect.TypeToken.of(com.datastax.driver.core.DataType.bigint().asJavaClass())

  def createCassandraSchema = {
    val schema = SchemaBuilder.createTable(CFKeys.playCassandra, CFKeys.geomatch).ifNotExists()
      .addPartitionKey("articleid", bigint)
      .addColumn("titlematch", com.datastax.driver.core.DataType.list(bigint))
      .addColumn("abstractmatch", com.datastax.driver.core.DataType.list(bigint))
      .addColumn("fulltextmatch", com.datastax.driver.core.DataType.list(bigint))
    session.execute(schema)
  }

  def buildParser(row: Row): GeoMatch = {

    val articleid = row.getLong("articleid")
    val titlematch: List[Long] = row.getList("titlematch", keysClass).asInstanceOf[List[Long]]
    val abstractmatch: List[Long] = row.getList("abstractmatch", keysClass).asInstanceOf[List[Long]]
    val fulltextmatch: List[Long] = row.getList("fulltextmatch", keysClass).asInstanceOf[List[Long]]

    GeoMatch(articleid, titlematch.toIndexedSeq, abstractmatch.toIndexedSeq, fulltextmatch.toIndexedSeq)
  }

  def getByIdF(articleid: Long): Future[IndexedSeq[GeoMatch]] = {
    val query = QueryBuilder.select().from(CFKeys.playCassandra, CFKeys.geomatch).allowFiltering().where(ceq("articleid", articleid))
    session.executeAsync(query) map (_.all().map(buildParser).toIndexedSeq)
  }

  def updateF(geo: GeoMatch) : Unit = {

    val preparedStatement = session.prepare( s"""UPDATE ${CFKeys.geomatch}
           set titlematch = ?,
           abstractmatch = ?,
           fulltextmatch = ?
           where articleid = ?;""")
    val titlematch: java.util.List[Long] = geo.titlematch.toList
    val abstractmatch: java.util.List[Long] = geo.titlematch.toList
    val fulltextmatch: java.util.List[Long] = geo.titlematch.toList

    val execFuture = session.executeAsync(preparedStatement.bind(titlematch, abstractmatch, fulltextmatch, geo.articleid.asInstanceOf[java.lang.Long]))
  }
}
