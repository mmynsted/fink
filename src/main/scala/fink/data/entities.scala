package fink.data

import com.github.nscala_time.time.Imports._

class Entity

object Entity {
  implicit class DateTimeOps(d: DateTime) {
    def toTimestamp = new java.sql.Timestamp(d.getMillis)
  }

  implicit class TimestampOps(t: java.sql.Timestamp) {
    def toDateTime = new DateTime(t)
  }
}

import Entity._

case class User(id: Long, name: String, password: String)

  /** maps to the Mtag table
    *
    * Note: Tag is a word used frequently by Slick 2.x
    * thus the tag table has been changed to Mtag to avoid
    * conflict and confusion.
    *
    * The Mtag table defines meta-tags use by various entities
    *
    * @param id of Mtag
    * @param name case insensitively unique name
    */
  case class Tag(id: Long, name: String) {
    def this (t: Tables.MtagRow) = this (t.id, t.name)
    def toRow: Tables.MtagRow = Tables.MtagRow(this.id, this.name)
  }

case class Category(id: Long, name: String) {
  def this(t: Tables.CategoryRow) = this (t.id, t.name)
  def toRow: Tables.CategoryRow = Tables.CategoryRow(this.id, this.name)
}

case class Post(id: Long, date: DateTime, title: String, author: String, shortlink: String, text: String,
                tags: Seq[Tag], categories: Seq[Category]) {
  def this(t: Tables.PostRow, tags: Seq[Tag], categories: Seq[Category]) =
    this (t.id, t.date.toDateTime, t.title, t.author, t.shortlink, t.text, tags, categories)

  def toRow: Tables.PostRow = Tables.PostRow(this.id, this.date.toTimestamp, this.title, this.author, this.shortlink, this.text)
}

  /** Page
    *
    * Page has a foreign component, a collection of [[fink.data.Tag]], which
    * represents a many-to-many relationship between the [[fink.data.Tables.Page]] table
    * and the [[fink.data.Tables.Mtag]] table.  The intersections are recorded using a joining table.
    *
    * @param id of page
    * @param date page was created or last modified
    * @param title title of page.  Could be used to update the HTML page title
    * @param author user with page ownership when page was created or modified
    * @param shortlink short, friendly URL
    * @param text Page text
    * @param tags tags for page categorization
    */
case class Page( id: Long, date: DateTime, title: String, author: String, shortlink: String, text: String,
                 tags: Seq[Tag]
){
    def this(t: Tables.PageRow, tags: Seq[Tag]) =
      this (t.id, t.date.toDateTime, t.title, t.author, t.shortlink, t.text, tags)

    /** convert to entity used by the Slick data model.
      *
      * @return [[fink.data.Tables.PageRow]] based on this entity
      */
    /*
      * Using the default auto-generated code results in a loss of fidelity mapping from [[fink.data.Page]] to
      * [[fink.data.Tables.PageRow]] because it does not include support a field for the foreign component for tags.
     */
    def toRow: Tables.PageRow =
      Tables.PageRow (this.id, this.date.toTimestamp, this.title, this.author, this.shortlink, this.text)
  }

/** Image entity
 *
 * @param id of image
 * @param date image was created or last modified
 * @param title simple title for the image
 * @param author user with image ownership when image was created or modified
 * @param hash unique hash to handle image duplication
 * @param contentType mime type of image
 * @param filename The image file name
 */
case class Image( id: Long, date: DateTime, title: String, author: String, hash: String, contentType: String,
  filename: String ) {
  def this(t: Tables.ImageRow) = this (t.id, t.date.toDateTime, t.title, t.author, t.hash, t.contenttype, t.filename)

  def toRow: Tables.ImageRow = Tables.ImageRow(this.id, this.date.toTimestamp, this.title, this.author, this.hash,
    this.contentType, this.filename)
}

case class Gallery(id: Long, date: DateTime, title: String, author: String,
                    shortlink: String, text: String, tags: Seq[Tag], images: Seq[Image], cover: Option[Image]) {
  def this(t: Tables.GalleryRow, tags: Seq[Tag], images: Seq[Image], cover: Option[Image]) =
  this (t.id, t.date.toDateTime, t.title, t.author, t.shortlink, t.text, tags, images, cover)

  def toRow: Tables.GalleryRow = {
    val maybeCoverId: Option[Long] = cover match {
      case Some(x) => Some(x.id)
      case _ => None
    }
    Tables.GalleryRow(this.id, maybeCoverId, this.date.toTimestamp, this.title, this.author, this.shortlink, this.text)
  }
}

/** base setting for the CMS
  *
  * @param id for the setting
  * @param title for the site
  * @param description of the site
  * @param keywords that describe the site
  * @param frontend TBD
  * @param categories TBD
  * @param uploadDirectory Where uploads are stored
  */
case class Setting(id: Long, title: String, description: String, keywords: String, frontend: String,
                     categories: String, uploadDirectory: String) {
  def this(t: Tables.SettingRow) = this (t.id, t.title, t.description, t.keywords, t.frontend, t.categories, t.uploaddirectory)
  def toRow: Tables.SettingRow = Tables.SettingRow(this.id, this.title, this.description, this.keywords, this.frontend,
  this.categories, this.uploadDirectory)
}


case class MonthKey(year: Int, month: Int, monthName: String)

