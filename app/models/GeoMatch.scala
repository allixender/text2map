package models

import java.util

import com.datastax.driver.core.DataType._
import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => ceq}
import com.datastax.driver.core.schemabuilder.SchemaBuilder

import service.{CFKeys, ConfigCassandraCluster}
import java.util.{Set, HashSet}

import scala.concurrent.Future

case class GeoMatch(
                     articleid: Long,
                     geonames: Set[Long]
                     )

object GeoMatch extends ConfigCassandraCluster {

  import cassandra.resultset._
  import play.api.libs.concurrent.Execution.Implicits._

  import scala.collection.JavaConversions._
  import scala.language.implicitConversions

  lazy val session = cluster.connect(CFKeys.playCassandra)

  val keysClass = com.google.common.reflect.TypeToken.of(Long.getClass)

  def createCassandraSchema = {
    val schema = SchemaBuilder.createTable(CFKeys.playCassandra, CFKeys.geomatch).ifNotExists()
      .addPartitionKey("articleid", bigint)
      .addColumn("geo_name_id", com.datastax.driver.core.DataType.set(bigint))
    session.execute(schema)
  }

  def buildParser(row: Row): GeoMatch = {

    val geo_name_id = row.getLong("articleid")
    val matches: Set[Long] = row.getSet("geo_name_id", keysClass).asInstanceOf[Set[Long]]
    GeoMatch(geo_name_id, matches)
  }

  def getByIdF(articleid: Long): Future[List[GeoMatch]] = {
    val query = QueryBuilder.select().from(CFKeys.playCassandra, CFKeys.geomatch).allowFiltering().where(ceq("articleid", articleid))
    session.executeAsync(query) map (_.all().map(buildParser).toList)
  }
}
