import org.scalatra._
import org.scalatra.servlet._

import javax.servlet.ServletContext
import javax.servlet.http.HttpServlet

import fink.web._
import fink.support._
import fink.data._

class ScalatraBootstrap extends LifeCycle with grizzled.slf4j.Logging{

  override def init(context: ServletContext) {
    info("Scalatra init")
    Repositories.init()
    val mTagRep = new TagRepository

    def printTag(mtag: Tag ): Unit = println(s"id = ${mtag.id}\tname=${mtag.name}")
    println(1)
    for (f <- mTagRep.findAll ) printTag(f)

    println(2)
    for (f <- mTagRep.byName("foo")) printTag(f)
    for (f <- mTagRep.byName("nothere")) printTag(f)

    println(3)
    for (f <- mTagRep.byId(-1L)) printTag(f)
    for (f <- mTagRep.byId(1L)) printTag(f)

    {
      val sRepository = new SettingRepository
      Config.init(sRepository.find)
    }

    context.mount(new Frontend, "/*")
    context.mount(new Admin, "/admin/*")
  }

  override def destroy(context: ServletContext) {
    info("Scalatra destroy")
    super.destroy(context)
    Repositories.shutdown()
  }
}

