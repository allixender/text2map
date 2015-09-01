package models

import com.datastax.driver.core.DataType._
import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => ceq}
import com.datastax.driver.core.schemabuilder.SchemaBuilder

import service.{CFKeys, ConfigCassandraCluster}
import java.util.UUID
import org.joda.time.DateTime

import scala.concurrent.Future

case class CheckSet(
                     articleid: Long,
                     artelement: String,
                     geonameid: Long,
                     oknotok: Boolean,
                     lastchange: DateTime
                     )

object CheckSet extends ConfigCassandraCluster {

  import cassandra.resultset._
  import play.api.libs.concurrent.Execution.Implicits._

  import scala.collection.JavaConversions._
  import scala.language.implicitConversions

  lazy val session = cluster.connect(CFKeys.playCassandra)

  def createCassandraSchema = {
    val schema = SchemaBuilder.createTable(CFKeys.playCassandra, CFKeys.checkset).ifNotExists()
      .addPartitionKey("articleid", bigint)
      .addClusteringColumn("artelement", text)
      .addClusteringColumn("geonameid", bigint)
      .addColumn("oknotok", cboolean)
      .addColumn("lastchange", timestamp)
    session.execute(schema)
  }

  def buildParser(row: Row): CheckSet = {

    val articleid = row.getLong("articleid")
    val artelement = row.getString("artelement")
    val geonameid = row.getLong("geonameid")
    val oknotok = row.getBool("oknotok")
    val lastchange = row.getDate("lastchange")

    CheckSet(articleid, artelement, geonameid, oknotok, new DateTime(lastchange))
  }

  def insertF(checkSet: CheckSet): Unit = {

    val preparedStatement = session.prepare( s"""INSERT INTO ${CFKeys.checkset}
         (articleid, artelement, geonameid, oknotok, lastchange)
         VALUES (?, ?, ?, ?, ?);""")

    val execFuture = session.executeAsync(preparedStatement.bind(
      checkSet.articleid.asInstanceOf[java.lang.Long], checkSet.artelement,
      checkSet.geonameid.asInstanceOf[java.lang.Long], checkSet.oknotok.asInstanceOf[java.lang.Boolean], checkSet.lastchange.toDate()))
  }
}
