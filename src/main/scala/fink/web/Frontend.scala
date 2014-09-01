package fink.web

import fink.data._
import com.github.nscala_time.time.Imports._
import org.scalatra._
import scalate.ScalateSupport
import org.fusesource.scalate.{ TemplateEngine, Binding }
import org.scalatra.{scalate, ApiFormats, ScalatraServlet}
import org.fusesource.scalate.layout.{DefaultLayoutStrategy, NullLayoutStrategy}
import collection.mutable

class Frontend extends ScalatraServlet
with ApiFormats
with ScalateSupport
with RepositorySupport
with MediaSupport {

  override val defaultLayoutPath = Some("/frontend/layouts/default.jade")
  override val defaultTemplatePath = List("/frontend")

  before() {
    contentType = formats("html")
  }

  get("/") {
    jade("index")
  }

  get("/:year/:month/:day/:shortlink/?") {
    val year = params("year").toInt
    val month = params("month").toInt
    val day = params("day").toInt
    val shortlink = params("shortlink")
    
    // postRepository.byShortlink(year, month, day, shortlink) match {
    postRepository.byShortlink(shortlink) match {
     case Some(post) => jade("post", "post" -> post)
     case None => halt(404, "Not found.")
    }
  }

//  // get("/:year/:month/?", params.getAs[Int]("year").isDefined && params.getAs[Int]("month").isDefined) {
//  get("/:year/:month/?") {
//    (for {
//      year <- params.getAs[Int]("year")
//      month <- params.getAs[Int]("month")
//    } yield {
//      postRepository.byMonth(month, year) match {
//        case Nil => halt(404, "Not found.")
//        case posts: List[_] =>
//          val date = new LocalDate(year, month, 1)
//          val formatter = DateTimeFormat.forPattern("MMMM")
//
//          jade("archive-month", "posts" -> posts, "year" -> year, "month" -> formatter.print(date))
//      }
//    }) getOrElse pass()
//  }

  get("/tag/:tag/?") {
    val tag = params("tag")
    postRepository.byTagName(tag) match {
      case Nil => halt(404, "Not found.")
      case posts: List[_] => jade("archive-tag", "posts" -> posts, "tag" -> tag)
    }
  }

  get("/page/:shortlink") {
    val shortlink = params("shortlink")

    pageRepository.byShortlink(shortlink) match {
      case Some(page) => jade("page", "page" -> page)
      case None => halt(404, "Not found.")
    }
  }

  get("/media/:shortlink") {
    galleryRepository.byShortlink(params("shortlink")) match {
      case Some(gallery) => jade("album", "gallery" -> gallery)
      case None => halt(404, "Not found.")
    }
  }

  notFound {
    contentType = null
    serveStaticResource() getOrElse halt(404, "Not found.")
  }

}
