package controllers

import play.api.libs.ws._
import play.api.Logger._
import play.api.mvc.{Action, Controller}
import play.api.cache._
import models.Article

import scala.concurrent.Future

object CassandraCtl extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.index("Text2Map article georeferencing experiment"))
  }

  def listArticles = Action.async {

    val allArticles = Article.getAllF

    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")
        Ok(views.html.ArticleList(artList))
    }
  }

  def mapEditArticlesById(articleid: Long) = Action.async {

    val allArticles = Article.getByIdF(articleid)
    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")
        Ok(views.html.FullArticle(artList.head))
    }
  }
}
