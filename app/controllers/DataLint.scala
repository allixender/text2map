package controllers

import play.api.libs.ws._
import play.api.Logger._
import play.api.mvc.{Action, Controller}
import models.Article

object DataLint extends Controller {

  val JOHNZ = "New Zealand Journal of Hydrology"
  val MARINE = "New Zealand Journal of Marine and Freshwater Research"

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
}
