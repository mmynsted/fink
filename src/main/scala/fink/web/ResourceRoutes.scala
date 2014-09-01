package fink.web

import fink.support._

import org.scalatra.servlet.FileUploadSupport
import org.scalatra.ScalatraServlet
import org.scalatra.json.{JacksonJsonSupport, JValueResult}

import scala.util.{Try, Success, Failure}

import org.json4s.jackson.Serialization.read
import com.github.nscala_time.time.Imports._

import fink.data._

trait ResourceRoutes extends ScalatraServlet
with RepositorySupport
with FileUploadSupport
with JacksonJsonSupport
with grizzled.slf4j.Logging {

  def getIdParameter(p: org.scalatra.Params): Try[Long] = {
    p.getAs[Long]("id") match {
      case Some(x: Long) => Success(x)
      case _ => {
        val ex = new Exception("Unable to create Long from given id parameter")
        error(ex)
        Failure(ex)
      }
    }
  }

  get("/api/settings") {
    settingRepository.find match {
      case Some(s) => s
      case _ => halt(500, "Internal error.")
    }
  }

  put("/api/settings") {
    val settings = read[Setting](request.body)
    settingRepository.update(settings) match {
      case Success(_) => {Config.init(settingRepository.byId(settings.id)); halt(204)}
      case _ => halt(500)
    }
  }

  /** get all API pages
    *
    */
  get("/api/pages") {
    pageRepository.findAll
  }

  //FIXME, see if "read" can be made safe
  post("/api/pages") {
    pageRepository.create(read[Page](request.body)) match {
      case Success(page) => page
      case Failure(ex) => halt(500, ex.getMessage)
      case _ => halt(500)
    }
  }

  get("/api/pages/:id") {
    (for {
      id <- params.getAs[Long]("id")
      page <- pageRepository.byId(id)
    } yield page) match {
      case Some(page) => page
      case _ => halt(404)
    }
  }

  //FIXME
  put("/api/pages/:id") {
    val page = read[Page](request.body)

    pageRepository.update(page) match {
      case Success(_) => halt(204)
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(404)
    }
  }

  delete("/api/pages/:id") {
    (for {
      id <- params.getAs[Long]("id")
      c <- (pageRepository.delete(id)).toOption
    } yield c) match {
      case Some(_) => halt(204)
      case _ => halt(404)
    }
  }

  get("/api/posts") {
    postRepository.findAll
  }

  //FIXME
  post("/api/posts") {
    val post = read[Post](request.body)

    postRepository.create(post) match {
      case Success(post) => post
      case _ => halt(500)
    }
  }

  get("/api/posts/:id") {
    (for {
      id <- params.getAs[Long]("id")
      post <- postRepository.byId(id)
    } yield post) match {
      case Some(post) => post
      case _ => halt(404)
    }
  }

  //FIXME
  put("/api/posts/:id") {
    val post = read[Post](request.body)

    postRepository.update(post)
    match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  delete("/api/posts/:id") {
    (for {
      id <- params.getAs[Long]("id")
      c <- postRepository.delete(id).toOption
    } yield c) match {
      case Some(_) => halt(204)
      case _ => halt(404)
    }
  }

  get("/api/posts/:id/tags") {
    postRepository.byId(params("id").toLong) match {
      case Some(post) => post.tags
      case None => halt(404)
    }
  }

  /** Add a tag to a post */
  // test this with bad id and bad tag name...
  post("/api/posts/:id/tags/:name") {
    (for {
      id <- params.getAs[Long]("id")
      post <- postRepository.byId(id)
      tag <- tagRepository.byName(params("name"))
      c <- postRepository.update(post.copy(tags=tag +: post.tags)).toOption
    } yield c) match {
      case None => halt(404)
      case _ => halt(204)
    }
  }

  delete("/api/posts/:id/tags/:name") {
    (for {
      id <- params.getAs[Long]("id")
      post <- postRepository.byId(id)
      c <- postRepository.update(post.copy(tags=post.tags.filterNot(_.name == params("name")))).toOption
    }yield c) match {
      case None => halt(404)
      case _ => halt(204)
    }
  }

  get("/api/posts/:id/categories") {
    (for {
      id <- params.getAs[Long]("id")
      post <- postRepository.byId(id)
    } yield post.categories) match {
      case Some(categories) => categories
      case _ => halt(404)
    }
  }

  put("/api/posts/:id/category/:name") {
    (for {
      id <- params.getAs[Long]("id")
      post <- postRepository.byId(id)
      category <- categoryRepository.byName(params("name"))
      c <- postRepository.update(post.copy(categories=category +: post.categories)).toOption
    } yield c) match {
      case None => halt(404)
      case _ => halt(204)
    }
  }

  get("/api/categories") {
    categoryRepository.findAll
  }

  get("/api/categories/:id") {
    (for {
      id <- params.getAs[Long]("id")
      category <- categoryRepository.byId(id)
    } yield category) match {
      case Some(category) => category
      case _ => halt(404)
    }
  }

  //FIXME unique constraint could cause a failure here, and it should be reported
  //Also read could be a problem
  post("/api/category") {
    val category = read[Category](request.body)
    (for {cat <- categoryRepository.create(category)} yield cat) match {
      case Success(category) => category
      case Failure(ex)=> halt(500, ex.getMessage)
    }
  }

  //The difference here between put and post is that
  //post creates and returns a new category and
  //put updates a category
  put("/api/categories/:id") {
    val category = read[Category](request.body)
    (for {cat <- categoryRepository.update(category)} yield cat) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  delete("/api/categories/:id") {
    (for {
      id <- getIdParameter(params)
      c <- categoryRepository.delete(id)
    } yield c) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  get("/api/tags") {
    tagRepository.findAll
  }

  get("/api/tags/:id") {
    (for {
      id <- params.getAs[Long]("id")
      t <- tagRepository.byId(id)
    } yield t) match {
      case Some(tag) => tag
      case _ => halt(404)
    }
  }

  //FIXME why create a Tag, then use only the name to create the Tag?
  post("/api/tags") {
    val tag = read[fink.data.Tag](request.body)
    (for {
      t <- tagRepository.create(tag.name)
    } yield t) match {
      case Success(tag) => tag
      case _ => halt(500)
    }
  }

  put("/api/tags/:id") {
    val tag = read[fink.data.Tag](request.body)
    (for {
     c <- tagRepository.update(tag)
    } yield c) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  delete("/api/tags/:id") {
    (for {
      id <- getIdParameter(params)
      c <- tagRepository.delete(id)
    } yield c) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  get("/api/galleries") {
    galleryRepository.findAll
  }

  post("/api/galleries") {
    val gallery = read[Gallery](request.body)
    (for {
      g <- galleryRepository.create(gallery)
    } yield g) match {
      case Success(gallery) => gallery
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(404)
    }
  }

  get("/api/galleries/:id") {
    for {
      id <- params.getAs[Long]("id")
    } yield galleryRepository.byId(id) match {
      case Some(gallery) => gallery
      case _ => halt(404)
    }
  }

  put("/api/galleries/:id") {
    val gallery = read[Gallery](request.body)
    galleryRepository.update(gallery) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  delete("/api/galleries/:id") {
    for {
      id <- getIdParameter(params)
    } yield galleryRepository.delete(id) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  get("/api/galleries/:id/cover") {
    for {
      id <- params.getAs[Long]("id")
      gallery <- galleryRepository.byId(id)
    } yield gallery.cover match {
      case Some(cover) => cover
      case _ => halt(404)
    }
  }

  get("/api/galleries/:id/images") {
    for {
      id <- params.getAs[Long]("id")
    } yield galleryRepository.byId(id) match {
      case Some(gallery: Gallery) => gallery.images
      case _ => halt(404)
    }
  }

  get("/api/images") {
    imageRepository.findAll
  }

  //FIXME This is totally unsafe
  post("/api/images") {
    //date: java.sql.Date, title: String, author: String, hash: String, contentType: String, filename: String)
    MediaManager.processUpload(fileParams("file")) match {
      case Some(ImageUpload(hash, contentType, filename)) =>
        imageRepository.create(new DateTime, "", "", hash, contentType, filename)
      case None => halt(500)
    }
  }

  get("/api/images/:id") {
    for {
      id <- params.getAs[Long]("id")
    } yield imageRepository.byId(id) match {
      case Some(image) => image
      case _ => halt(404)
    }
  }

  //FIXME this is also unsafe.
  put("/api/images/:id") {
    val image = read[Image](request.body)
    imageRepository.update(image) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }

  delete("/api/images/:id") {
    for {
      id <- getIdParameter(params)
    } yield imageRepository.delete(id) match {
      case Failure(ex) => halt(404, ex.getMessage)
      case _ => halt(204)
    }
  }
}
