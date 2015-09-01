package controllers

import play.api.libs.ws._
import play.api.Logger._
import play.api.mvc.{Action, Controller}
import models.{GeoMatch, Article}

object DataLint extends Controller {

  val JOHNZ = "New Zealand Journal of Hydrology"
  val MARINE = "New Zealand Journal of Marine and Freshwater Research"
  val GEOLOGY = "New Zealand Journal of Geology and Geophysics"

  val journals = List(JOHNZ, MARINE, GEOLOGY)

  def lintJOHNZurls(url: String) : String = {
    // TODO add baseurl to Article URL number
    val linted = s"http://hydrologynz.co.nz/journal.php?article_id=$url"
    logger.debug(s"linting $url >>> $linted")
    linted
  }

  def lintRoyalUrls(url: String) : String = {
    // TODO change from pdf URL to Abstract Ulr
    val linted = url.replace("pdf","abs")
    logger.debug(s"linting $url >>> $linted")
    linted
  }

  def lintUrl(article: Article) : Article = {
    if (article.journal.equals(JOHNZ)) {
      new Article(
        article.articleid,
        article.journal,
        article.authortitle,
        article.textabs,
        article.author,
        article.title,
        article.year,
        lintJOHNZurls(article.arturl),
        article.fulltext)
    } else {
      if (article.journal.equals(MARINE)) {
        new Article(
          article.articleid,
          article.journal,
          article.authortitle,
          article.textabs,
          article.author,
          article.title,
          article.year,
          lintRoyalUrls(article.arturl),
          article.fulltext)
      } else {
        article
      }
    }
  }

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def lintOnce = Action.async {

    val allArticles = Article.getAllF
    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")
        artList.map{
          article =>
            val linted = lintUrl(article)
            Article.updateF(linted)
            logger.info(s"linting ${linted.articleid}")
        }
        Ok(views.html.index(s"got ${artList.length} elements for linting, please check the log"))
    }
  }

  def redeemArticles = Action.async {

    val allArticles = Article.getAllF

    allArticles.map {
      artList =>
        logger.info(s"redeem ru ngot ${artList.length} elements")
        artList.foreach {
          art =>
            val a1 = art.articleid

            val a2 = journals.contains(art.journal)
            val a3 = art.authortitle != null
            val a4 = art.textabs != null
            val a5 = art.author != null
            val a6 = art.title != null

            val a7 = !(art.year == 0)

            val a8 = art.arturl != null && !art.arturl.isEmpty
            logger.debug(art.arturl)

            val a9 = art.fulltext != null

            if (!a2) {
              logger.warn(s"check journal ${a1} ${art.journal}")
            }
            if (!a3) {
              logger.warn(s"check authortitle ${a1}")
            }
            if (!a4) {
              logger.warn(s"check textabs ${a1} ")
            }
            if (!a5) {
              logger.warn(s"check author ${a1}")
            }
            if (!a6) {
              logger.warn(s"check title ${a1}")
            }
            if (!a7) {
              logger.warn(s"check year ${a1}")
            }
            if (!a8) {
              logger.warn(s"check arturl ${a1}")
            }
            if (!a9) {
              logger.warn(s"check fulltext ${a1}")
            }
        }

        Ok(views.html.index("redeem run, check log"))
    }
  }

  def createSchemas = Action {

    GeoMatch.createCassandraSchema

    Ok(views.html.index("schemas run"))
  }
}
