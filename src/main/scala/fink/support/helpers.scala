package fink.support

import org.fusesource.scalate.servlet.ServletRenderContext._
import org.joda.time.format._
import fink.data._

object DateHelper {

  protected lazy val fmt         = DateTimeFormat.forPattern("yyyy-MM-dd")
  protected lazy val yearFormat  = DateTimeFormat.forPattern("yyyy")
  protected lazy val monthFormat = DateTimeFormat.forPattern("MM")
  protected lazy val dayFormat   = DateTimeFormat.forPattern("dd")

  protected val formats = collection.mutable.Map[String, DateTimeFormatter]()

  def formatDate(date: Long) = {
    fmt.print(date)
  }

  def formatDate(date: Long, fs: String) = {
    val fmt = formats.getOrElse(fs, {
      val x = DateTimeFormat.forPattern(fs)
      formats(fs) = x
      x
    })
    fmt.print(date)
  }

  def day(date: Long) = {
    dayFormat.print(date)
  }

  def month(date: Long) = {
    monthFormat.print(date)
  }

  def year(date: Long) = {
    yearFormat.print(date)
  }

}

object Config {

  private var _setting: Setting = null
  var maybeSetting: Option[Setting] = None

  def init(s: Option[Setting]) = {
    maybeSetting = s
    s.map(_setting = _)
  }

  def setting: Setting = _setting

  def mediaDirectory = {
    setting.uploadDirectory
  }

}

object TemplateHelper extends RepositorySupport {

  import DateHelper._

//  def postUri(post: Post) = {
//    renderContext.uri("/%s/%s/%s/%s/".format(year(post.date), month(post.date), day(post.date), post.shortlink))
//  }

  def slug(s: String) = {
    s.toLowerCase.replaceAll("""[^a-z0-9\s-]""", "")
      .replaceAll("""[\s-]+""", " ").trim
      .replaceAll("""\s""", "-")
  }

  def setting = Config.setting

  def maybeSetting = Config.maybeSetting

  def defaultTitle: String = {
    maybeSetting match {
      case Some(s: Setting) => s.title
      case _ => "undefined"
    }
  }
}
