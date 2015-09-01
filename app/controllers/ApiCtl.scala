package controllers

import models.{Article, GeoMatch, GeoName}
import play.api.Logger._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json.{JsPath, Reads, Writes, _}
import play.api.mvc.{Action, Controller}

object ApiCtl extends Controller {


  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  // TODO, we might need different case classes for send and receive messages
  // particularly because of UUID generation/handling
  case class ArtJsIn(
                      subject: String,
                      message: String,
                      usernamefrom: String,
                      usernameto: String)

  // no fulltext or abstract
  case class ArtMetadaJsOut(
                             articleid: Long,
                             journal: String,
                             authortitle: String,
                             author: String,
                             title: String,
                             year: Long,
                             arturl: String)

  implicit val artJsReads: Reads[ArtJsIn] = (
    (JsPath \ "subject").read[String] and
      (JsPath \ "message").read[String] and
      (JsPath \ "usernamefrom").read[String](minLength[String](3)) and
      (JsPath \ "usernameto").read[String](minLength[String](3)))(ArtJsIn.apply _)

  implicit val artMetaJsWrites: Writes[ArtMetadaJsOut] = (
    (JsPath \ "articleid").write[Long] and
      (JsPath \ "journal").write[String] and
      (JsPath \ "authortitle").write[String] and
      (JsPath \ "author").write[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "year").write[Long] and
      (JsPath \ "arturl").write[String])(unlift(ArtMetadaJsOut.unapply))

  implicit val geonameJsWrites: Writes[GeoName] = (
    (JsPath \ "name_id").write[Long] and
      (JsPath \ "name").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "land_district").write[String] and
      (JsPath \ "crd_projection").write[String] and
      (JsPath \ "crd_north").write[Double] and
      (JsPath \ "crd_east").write[Double] and
      (JsPath \ "crd_datum").write[String] and
      (JsPath \ "crd_latitude").write[Double] and
      (JsPath \ "crd_longitude").write[Double]
    )(unlift(GeoName.unapply))

  def articleToJsonClass(article: Article): ArtMetadaJsOut = {
    ArtMetadaJsOut(
      article.articleid,
      article.journal,
      article.authortitle,
      article.author,
      article.title,
      article.year,
      article.arturl)
  }

  def allArticles = Action.async {

    val allArticles = Article.getAllF

    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")
        val jsonList = artList.map { article =>
          Json.toJson(articleToJsonClass(article))
        }
        Ok(Json.toJson(jsonList))
    }
  }

  def articleById(articleid: Long) = Action.async {

    val allArticles = Article.getByIdF(articleid)
    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")

        Ok(Json.toJson(articleToJsonClass(artList.head)))
    }
  }

  def getAbstractPlainText(articleid: Long) = Action.async {

    val allArticles = Article.getByIdF(articleid)
    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")

        Ok(artList.head.textabs).as("text/plain")
    }
  }

  def getFullPlainText(articleid: Long) = Action.async {

    val allArticles = Article.getByIdF(articleid)
    allArticles.map {
      artList =>
        logger.info(s"got ${artList.length} elements")

        Ok(artList.head.textabs).as("text/plain")
    }
  }

  def getMatchesForArticle(matchid: Long) = Action.async {

    val allMatches = GeoMatch.getByIdF(matchid)

    allMatches.map { geomatches =>
      logger.info(s"got ${geomatches.size} elements")

      val jsonList = geomatches.map { singleMatch =>
        val jsonArray1 = Json.toJson(singleMatch.titlematch)
        val jsonArray2 = Json.toJson(singleMatch.abstractmatch)
        val jsonArray3 = Json.toJson(singleMatch.fulltextmatch)
        val jsonObject = Json.obj(
          "articleid" -> JsNumber(singleMatch.articleid),
          "titlematch" -> jsonArray1,
          "abstractmatch" -> jsonArray2,
          "fulltextmatch" -> jsonArray3
        )
        jsonObject
      }
      Ok(Json.toJson(jsonList))
    }
  }

  def getAllGeoNames = Action.async {

    val allGeoNames = GeoName.getAllF

    allGeoNames.map {
      geonames =>
        logger.info(s"got ${geonames.length} elements")
        val jsonList = geonames.map { geoname =>
          Json.toJson(geoname)
        }
        Ok(Json.toJson(jsonList))
    }
  }

  def getGeoNameById(name_id: Long) = Action.async {

    val allGeoNames = GeoName.getByIdF(name_id)
    allGeoNames.map {
      geonames =>
        logger.info(s"got ${geonames.length} elements")

        Ok(Json.toJson(geonames.head))
    }
  }

}
