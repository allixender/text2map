package models

import com.datastax.driver.core.DataType._
import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => ceq}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import service.{CFKeys, ConfigCassandraCluster}

import scala.concurrent.Future

case class Article(
  articleid: Long,
  journal: String,
  authortitle: String,
  textabs: String,
  author: String,
  title: String,
  year: Long,
  arturl: String,
  fulltext: String
)

object Article extends ConfigCassandraCluster {

  import cassandra.resultset._
  import play.api.libs.concurrent.Execution.Implicits._

  import scala.collection.JavaConversions._
  import scala.language.implicitConversions

  lazy val session = cluster.connect(CFKeys.playCassandra)

  def createCassandraSchema = {
    val schema = SchemaBuilder.createTable(CFKeys.playCassandra, CFKeys.articles).ifNotExists()
      .addPartitionKey("articleid", bigint)
      .addClusteringColumn("journal", text)
      .addColumn("authortitle", text)
      .addColumn("textabs", text)
      .addColumn("author", text)
      .addColumn("title", text)
      .addColumn("year", bigint)
      .addColumn("arturl", text)
      .addColumn("fulltext", text)
    session.execute(schema)
  }

  def buildParser(row: Row) : Article = {

    val articleid = row.getLong("articleid")
    val authortitle = row.getString("authortitle")
    val journal = row.getString("journal")
    val textabs = row.getString("textabs")
    val author = row.getString("author")
    val title = row.getString("title")
    val year = row.getLong("year")
    val arturl = row.getString("arturl")
    val fulltext = row.getString("fulltext")

    Article(
      articleid,
      journal,
      authortitle,
      textabs,
      author,
      title,
      year,
      arturl,
      fulltext)
  }

  def insertF(abs: Article) : Unit = {

    val preparedStatement = session.prepare( s"""INSERT INTO ${CFKeys.articles}
           (articleid,journal,authortitle,textabs,author,title,year,arturl,fulltext)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);""")

    val execFuture = session.executeAsync(preparedStatement.bind(
      abs.articleid.asInstanceOf[java.lang.Long], abs.journal, abs.authortitle,
      abs.textabs, abs.author, abs.title, abs.year.asInstanceOf[java.lang.Long], abs.arturl, abs.fulltext))
  }

  /**
   * Cassandra delete Refactor, fire and forget
   */
  def updateF(abs: Article) : Unit = {

    val preparedStatement = session.prepare( s"""UPDATE ${CFKeys.articles}
           set
           authortitle = ?,
           textabs = ?,
           author = ?,
           title = ?,
           year = ?,
           arturl = ?,
           fulltext = ?
           where articleid = ? and journal = ?;""")

    val execFuture = session.executeAsync(preparedStatement.bind(
      abs.authortitle,
      abs.textabs, abs.author, abs.title, abs.year.asInstanceOf[java.lang.Long],
      abs.arturl, abs.fulltext, abs.articleid.asInstanceOf[java.lang.Long], abs.journal))
  }

  /**
   * Cassandra delete Refactor, fire and forget
   */
  def deleteF(abs: Article) = {
    val query = QueryBuilder.delete().from(CFKeys.playCassandra, CFKeys.articles).where(ceq("articleid", abs.articleid))
    session.executeAsync(query)
  }

  /**
   * Refactor Cassandra
   */
  def getAllF: Future[List[Article]] = {
    val query = QueryBuilder.select().all().from(CFKeys.playCassandra, CFKeys.articles).allowFiltering()
    session.executeAsync(query) map (_.all().map(buildParser).toList)
  }

  /**
   * Refactor Cassandra
   */
  def getByIdF(articleid: Long): Future[List[Article]] = {
    val query = QueryBuilder.select().from(CFKeys.playCassandra, CFKeys.articles).allowFiltering().where(ceq("articleid", articleid))
    session.executeAsync(query) map (_.all().map(buildParser).toList)
  }
}
