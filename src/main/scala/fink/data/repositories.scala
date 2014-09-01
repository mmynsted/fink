package fink.data

//import fink.support._

import org.scalatra.ScalatraServlet

import scala.slick.driver.PostgresDriver.simple._

import com.mchange.v2.c3p0.ComboPooledDataSource
import fink.support.TemplateHelper

//import java.util.Properties
import scala.util.{Try, Success, Failure}
//import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import Entity._

/** Repositories are components that provide data access logic for a part of the data model. */
object Repositories extends AnyRef with grizzled.slf4j.Logging {

  ////Map between org.joda.time.DateTime and java.sqlTimestamp
  //implicit val mappedDateTime = MappedColumnType.base[DateTime, java.sql.Timestamp](
  //{d => new java.sql.Timestamp(d.getMillis)}, // map DateTime to Timestamp
  //{t => new DateTime(t)} // map Timestamp to DateTime
  //)

  /** Create path to dataStore */
  def init() {
    info("init dataStore")

    val selectedSource = scala.util.Properties.envOrElse("cpds", "default")
    info(s"Using ComboPooledDataSource: $selectedSource")

    _cpds = for {
      selectedSourceInfo <- fink.BuildInfo.dataSourceInfo get selectedSource
      driverClass <- selectedSourceInfo get 'jdbcDriver
      jdbcUrl <- selectedSourceInfo get 'url
      user <- selectedSourceInfo get 'user
      password <- selectedSourceInfo get 'password
    } yield {
      val c = new ComboPooledDataSource(selectedSource)
      c.setDriverClass(driverClass)
      c.setJdbcUrl(jdbcUrl)
      c.setUser(user)
      c.setPassword(password)
      c
    }
    _db = Some(Database.forDataSource(_cpds.getOrElse(throw new Error("Failure creating ComboPooledDataSource"))))

  }

  private var _cpds : Option[ComboPooledDataSource] = None
  private var _db : Option[Database] = None

  def db = _db.getOrElse(throw new Error("Not initialized."))

  /** Close path to dataStore */
  def shutdown() {
    info("shutdown dataStore")

    _cpds map (_.close)
    _cpds = None
    _db = None
  }

    val pageRepository     = new PageRepository
    val pageTagJoinRepository = new PageTagJoinRepository
    val postRepository     = new PostRepository
    val postTagJoinRepository = new PostTagJoinRepository
    val postCategoryJoinRepository = new PostCategoryJoinRepository
    val tagRepository      = new TagRepository
    val categoryRepository = new CategoryRepository
    val imageRepository    = new ImageRepository
    val galleryRepository  = new GalleryRepository
    val galleryTagJoinRepository = new GalleryTagJoinRepository
    val galleryImageJoinRepository = new GalleryImageJoinRepository
    val settingRepository = new SettingRepository
}

/** common logic required to support data access */
trait RepositorySupport extends AnyRef with grizzled.slf4j.Logging {
  /** used as a placeholder for Long based pk values that are ignored by Slick
    */
  final val IgnoredLong = 0L

  //FIXME why are these def rather than lazy val?
  def pageRepository     = Repositories.pageRepository
  def pageTagJoinRepository = Repositories.pageTagJoinRepository
  def postRepository     = Repositories.postRepository
  def postTagJoinRepository = Repositories.postTagJoinRepository
  def postCategoryJoinRepository = Repositories.postCategoryJoinRepository
  def tagRepository      = Repositories.tagRepository
  def categoryRepository = Repositories.categoryRepository
  def imageRepository    = Repositories.imageRepository
  def galleryRepository  = Repositories.galleryRepository
  def galleryTagJoinRepository = Repositories.galleryTagJoinRepository
  def galleryImageJoinRepository = Repositories.galleryImageJoinRepository
  def settingRepository = Repositories.settingRepository

//
  def db = Repositories.db

  /* Common messages
   */
  def emptyUpdateWarning(entity: String): String =
    StringContext("A \'", "\' update was attempted, but did not alter any records.  " +
      "Perhaps an insert was expected?").s(entity)

  def updateError(entity: String, throwable: Throwable): String =
    StringContext("Error updating \'", "\' . ").s(entity) + throwable.getMessage()

  def deleteError(entity: String, throwable: Throwable): String =
    StringContext("Error deleting \'", "\' . ").s(entity) + throwable.getMessage()

  def wrongCountDeleteWarning(entity: String): String =
  StringContext("A \'", """\' delete was attempted, but too many or too few records
    | were processed.""".stripMargin).s(entity)


  /** pieces of queries or full queries
    *
    * Simplify the execution of queries where an explicit transaction
    * may or may not be needed.
    *
    */
  protected class QueryHelpers {

    def createPageMtagJoin(pageId: Long, tagId: Long)(implicit session: Session): Try[Long] =
      Try {(Tables.PageMtag returning Tables.PageMtag.map(_.id)) +=
        Tables.PageMtagRow(IgnoredLong, pageId, tagId)}

    def createMtag(name: String)(implicit session: Session): Try[Long] =
      Try{(Tables.Mtag returning Tables.Mtag.map(_.id)) +=
        Tables.MtagRow(IgnoredLong, name)}

    def deletePageMtagJoin(id: Long)(implicit session: Session): Try[Int] =
    Try {Tables.PageMtag.filter(_.id === id).delete}

    def createPage(date: DateTime, title: String, author: String,
                   shortlink: String, text: String)(implicit session: Session): Try[Long] =
      Try{(Tables.Page returning Tables.Page.map(_.id)) +=
        Tables.PageRow(IgnoredLong, date.toTimestamp, title, author, shortlink, text)}
  }

  protected val queryHelpers = new QueryHelpers

  def wError(s: String): Exception = {
    error(s)
    new Exception(s)
  }
}

/*
 * Blocks defined for db withTransaction will automatically rollback on an exception at the end of the block,
 * thus a session.rollback() is not needed to protect from partial commit on failure.
 *
 * Warning: Blocks defined with db withSession will "auto-commit".
 *
 */

//
//object DBUtil {
//  def insertId = Query(SimpleFunction.nullary[Long]("scope_identity")).first
//}
//
//object UserRepository extends RepositorySupport {
//  def find(name: String) = {
//    Some(User(0, "name", "password"))
//  }
//
//  def login(name: String, password: String) = Some(User(0, "name", "password"))
//}
//

object UserRepository extends RepositorySupport {
  def find(name: String) = Some(User(0, "name", "password"))
  def login(name: String, password: String) = Some(User(0, "name", "password"))
}

/** page data access */
class PageRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "Page"

  /** convert an Option[Long], Mtag id, to a [[fink.data.Tag]] entity
    *
    * @param mtagId [[fink.data.Tables.Mtag]] id
    * @return a new [[fink.data.Tag]] entity
    */
  protected def getTag(mtagId: Option[Long]): Option[fink.data.Tag] =
    mtagId.flatMap((tagRepository.byId(_)))


  /** get a [[fink.data.Page]] loaded with [[fink.data.Tag]]s
    *
    * @param pageRow [[fink.data.Tables.PageRow]]
    * @param mtagIds a sequence of [[fink.data.Tables.Mtag]] ids
    * @return completed [[fink.data.Page]]
    */
  protected def getPage(pageRow: Tables.PageRow, mtagIds: Seq[Option[Long]]): fink.data.Page = {
    val tag = for {
      maybeId <- mtagIds
      t <- getTag(maybeId)
    } yield t
    new fink.data.Page(pageRow, tag)
  }

  /** compose completed scala.Seq[fink.data.Page] from partial results
    *
    * The input Seq[(PageRow, Option[Long])], represents one or more [[fink.data.Tables.PageRow]]s
    * each with zero or more [[fink.data.Tables.Mtag]] ids.
    *
    * Because it is possible to have more than one Mtag for a given PageRow, it
    * is possible to have more than one instance of the the same PageRow.
    *
    * Grouping is then handled in this method to produce a [[fink.data.Page]]
    * with the correct Seq of [[fink.data.Tag]] components.  At some future date
    * it may be feasible to handle many-to-many, left-outer-joins using lifted Slick.
    *
    * @note Do not expect the original order of the fetch to be preserved.
    *
    * @param partPages represents one or more [[fink.data.Tables.PageRow]],
    *                  each with zero or more [[fink.data.Tables.Mtag]] ids.
    * @return [[fink.data.Page]] loaded with [[fink.data.Tag]] entities
    */
  protected def load(partPages: Seq[(Tables.PageRow, Option[Long])]): Seq[Page] = {
    //make Map with PageRow as key and mtag id as value
    val groupedPagesMap = partPages.groupBy(_._1) mapValues (_ map (_._2))

    //construct the result
    groupedPagesMap.toSeq.foldLeft(Seq[Page]() )( (b,a) => getPage(a._1, a._2) +: b)
  }

  /** find all the pages then load each page with its components
    *
    * @return Seq of loaded Pages, that is pages
    *         that contain applicable Tag entries
    */
  def findAll : Seq[Page] = db withSession {
    implicit session => val q = for {
      (p, j) <- Tables.Page leftJoin Tables.PageMtag on (_.id === _.pageId)
    } yield (p, j.mtagId.?)
      load(q.list())
  }

  /** optionally find the [[fink.data.Page]] element based on supplied id
    *
    * @param id is a [[fink.data.Tables.Page]] id
    * @return [[fink.data.Page]] if found
    */
  def byId(id: Long) : Option[Page] = db withSession {
    implicit session => {
      val pageByIdC = {
        def q(id: Column[Long]) = for {
          (p, j) <- Tables.Page leftJoin Tables.PageMtag on (_.id === _.pageId)
          if id === p.id
        } yield (p, j.mtagId.?)
        Compiled(q _)
      }
      /* Use compiled query to gather elements from left outer join
       *
       * Create Page element from results and collect first element from collection
       * (which should have one element) by collecting against a partial function
       * checking for a Page with the correct id
       */
      load(pageByIdC(id).list()).collectFirst({case x: Page if x.id == id => x})
    }
  }

  /** optionally find the [[fink.data.Page]] element based on supplied shortlink
    *
    * @param shortlink is a [[fink.data.Tables.Page]] shortlink
    * @return [[fink.data.Page]] if found
    */
  def byShortlink(shortlink: String) : Option[Page] = db withSession {
    implicit session => {
      val pageByShortlinkC = {
        def q(shortlink: Column[String]) = for {
          (p, j) <- Tables.Page leftJoin Tables.PageMtag on (_.id === _.pageId)
          if shortlink === p.shortlink
        } yield (p, j.mtagId.?)
        Compiled(q _)
      }
      load(pageByShortlinkC(shortlink).list()).collectFirst({case x: Page if x.shortlink == shortlink => x})
    }
  }

  /** create new [[fink.data.Tables.Page]]
    *
    * @note Page id is ignored both by this method and by SLICK on insert
    *           because the column is __scala.slick.ast.ColumnOption.AutoInc__
    * @return The id of the newly created Page
    */
  //FIXME this is awkward
  def create(date: DateTime, title: String, author: String,
             shortlink: String, text: String, tags: Seq[Tag]): Try[Long] = db withTransaction {
    implicit session => {
      queryHelpers.createPage(date, title, author, shortlink, text) match {
        case x @ Success(id) => val s = tags.foldLeft(Set[Tables.PageMtagRow]())((b,a) =>
          b + Tables.PageMtagRow(IgnoredLong, id, a.id))
          Try {(Tables.PageMtag returning Tables.PageMtag.map(_.id)) ++= s}; x
        case x @ Failure(ex) => {error(s"Failed during creation of new Page, rolling back... $ex"); x}
      }
    }
  }

  def create(id: Long, date: DateTime, title: String, author: String, shortlink: String,
             text: String, tags: Seq[Tag]): Try[Long] = create(date, title, author, shortlink, text, tags)

  def create(page: Page): Try[Long] = create(page.date, page.title, page.author, page.shortlink,
    page.text, page.tags)

  def update(page: Page) = db withTransaction {
    implicit session =>
      //re-compute the shortlink then update the fields
       Try {byId(page.id).map { p =>
        Tables.Page.filter(_.id === page.id).update(page.copy(shortlink = TemplateHelper.slug(page.title)).toRow)

        /* Tag (Mtag) entities are foreign components so they are handled separately
        */

        // remove all tags for p (persisted version of Page) that are not found with local version.
        p.tags.filterNot(page.tags.contains).foreach(tag => removeTag(page.id, tag.name))

        // add all tags found in local page that are not associated with persisted version
        page.tags.filterNot(p.tags.contains).foreach(tag => addTag(page.id, tag.name))
      }} match {
         case x @ Success(_) => x
         case x @ Failure(ex) => {error(updateError(Ent, ex)); x}
       }
  }

  /** naive delete
    *
    * @param id
    * @return Try of deleted record count
    */
  def delete(id: Long): Try[Int] = db withTransaction {
    implicit session => Try {(Tables.Page filter(_.id === id)).delete} match {
      case x @ Success(count: Int) if count == 1 => x
      case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
      case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
    }
  }

  /** logically relate a [[fink.data.Tables.Mtag]] to a [[fink.data.Tables.Page]]
    *
    * @note It is possible for this method to create two new entities,
    *       Mtag and or [[fink.data.Tables.PageMtag]].  It may roll-back on
    *       error.  An example error would be if the supplied Page id does not exist, as
    *       this would violate a foreign key constraint on the PageMtag table.
    *
    * @param pageId valid Page id
    * @param tagName of the tag to associate
    * @return Try[Long] representing the id of the newly created PageMtag join
    */
  def addTag(pageId: Long, tagName: String): Try[Long] = db withTransaction {
    /* TODO: Clean this up.  May want to add a recover on the creation of the
     *       PageMtag to attempt to return an __existing__ join if applicable.
     */
    implicit session =>
        val maybeMtag: Option[Long] = (for {
          mtag <- Tables.Mtag.filter(_.name === tagName)
        } yield mtag.id).firstOption

      maybeMtag.fold[Try[Long]](tagRepository.create(tagName))(Success(_)) match {
        case x @ Success(mtagId) => pageTagJoinRepository.create(pageId, mtagId)
        case x @ Failure(_) => x
      }
  }

  /** remove [[fink.data.Tables.Mtag]] from [[fink.data.Tables.Page]]
    *
    * @param id Page id
    * @param tagName Mtag name
    * @return Try of count of deleted records
    */
  def removeTag(id: Long, tagName: String): Try[Int] = {
   pageTagJoinRepository.removeTagFromPage(id, tagName)
  }
}

class PostRepository extends RepositorySupport {
  final val Ent: String = "Post"
 // import fink.support.DateHelper._
//
//
//  def byMonth: Map[(Int, Int, String), Seq[Post]] = {
//    def postDateTuple(p: Post) = (formatDate(p.date, "Y").toInt, formatDate(p.date, "M").toInt, formatDate(p.date, "MMMM"))
//    findAll.map(p => (postDateTuple(p), p)).groupBy(_._1).mapValues(s => s.map(_._2)).toList.sortBy(_._1).toMap
//  }
//

  def byMonth(month: Int, year: Int): Seq[Post] = db withSession {
   implicit session =>
     val base = new DateTime(year, month, 1, 0, 0)
     val from = base.toTimestamp
     val to   = (base.plusMonths(1)).toTimestamp
    load(Tables.Post.filter(_.date between(from, to)).buildColl[IndexedSeq])
  }

  /** get a Map of all Posts
    *
    * Key: (year, month, month name)
    * Value: Seq[Post]
    */
  def byMonth: Map[MonthKey, Seq[Post]] = db withSession {
    implicit session =>
      def getMonthKey(p: Post): MonthKey = {
        val d = p.date
        MonthKey(d.getYear, d.getMonthOfYear, d.month.getAsString)
      }
      load(Tables.Post.sortBy(_.date.desc).buildColl[IndexedSeq]).groupBy(p => getMonthKey(p))
  }

  protected def load(partPosts: Seq[Tables.PostRow]) = {
    partPosts.map(p =>
      new Post(p, postTagJoinRepository.tagsByPostId(p.id), postCategoryJoinRepository.categoriesByPostId(p.id)))
  }

  def findAll = db withSession {
    implicit session => load(Tables.Post.buildColl[IndexedSeq])
  }

  def byId(id: Long) = db withSession {
    implicit session => val postByIdC = Tables.Post.findBy(_.id)
      load(postByIdC(id).firstOption.toSeq) match {
        case x::nil => Some(x)
        case _ => None
      }
  }

  def byTag(tag: Tag) = byTagName(tag.name)

  def byTagName(name: String) = db withSession {
    implicit session => val byTagNameC = {
      def q(name: Column[String]) = for {
        t <- Tables.Mtag filter(_.name === name)
        j <- Tables.PostMtag filter(_.mtagId === t.id)
        p <- Tables.Post filter(_.id === j.postId)
      } yield p
      Compiled(q _)
    }
      load(byTagNameC(name).buildColl[IndexedSeq])
  }

  def byShortlink(shortlink: String) = db withSession {
    implicit session => val postByShortlinkC = Tables.Post.findBy(_.shortlink)
      load(postByShortlinkC(shortlink).firstOption.toSeq) match {
        case x::nil => Some(x)
        case _ => None
      }
  }

  def create(post: Post): Try[Post] = db withTransaction {
    implicit session => val insertedKey = Try[Long] (Tables.Post returning Tables.Post.map(_.id) += post.toRow) match {
      case x: Success[Long] => x
      case x @ Failure(ex) => {error(s"Error creating $Ent with title \'${post.title}\'. $ex"); x}
    }
      insertedKey.flatMap(byId(_) match {
        case Some(x) => Success(x)
        case _ => Failure(wError(s"Unknown error creating $Ent with title \'${post.title}\'"))
      })
  }

  def update(post: Post) = db withTransaction {
    implicit session =>
     lazy val uTags = {//tags

      // must split out difference between supplied (s) and persisted (p)data
      val q = Tables.PostMtag.filter(_.postId === post.id)

      //complement of p \ s, i.e. the items in p not in s must be removed from p
      //records in PostMtag for the given post id where the mtagId is not found in post.tags
      q.filterNot(_.mtagId inSet post.tags.map(_.id)).delete

      //intersection, c ∩ p, need not be changed because it contains only join logic.

      //complement of s \ p, i.e. the items in s not in p must be inserted into p
      //insert into PostMtag each post.tags element not found in PostMtag for the given post id
      val ptJoin = q.buildColl[Set]
      val newTags = post.tags.filterNot(s => ptJoin.exists(_.mtagId == s.id))
      Tables.PostMtag ++= newTags map (x => Tables.PostMtagRow(IgnoredLong, post.id, x.id))
    }
    lazy val uCategories = { //categories

      // must split out difference between supplied (s) and persisted (p) data
      val q = Tables.PostCategory.filter(_.postId === post.id)

      //complement of p \ s, i.e. the items in p not in s must be removed from p
      //records in PostCategory for the given post id where categoryId is not found in post.categories
      q.filterNot(_.categoryId inSet post.categories.map(_.id)).delete

      //intersection, c ∩ p
      //PostCategory is join only, nothing to update

      //complement of s \ p, i.e. the items in s not in p must be inserted into p
      //insert into PostCategory each post.categories element not found in PostCategory for the given post id
      val ptJoin= q.buildColl[Set]
      val newCategories = post.categories.filterNot(s => ptJoin.exists(_.categoryId == s.id))
      Tables.PostCategory ++= newCategories map(x => Tables.PostCategoryRow(IgnoredLong,post.id, x.id))
    }
    val result = for {
        _ <-Try{uTags}
        rC <- Try{uCategories}
      } yield rC

      result match {
        case Failure(ex) => error(s"Error updating $Ent with id \'${post.id}\'. $ex")
        case _ =>
      }

      result
  }

  def delete(id: Long): Try[Int] = db withTransaction {
    implicit session =>
    //tag relationship
      lazy val tagJoin = {
        val postMTagByIdD = Tables.PostMtag.findBy(_.postId)
        Try[Int](postMTagByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting PostMTag with post id \'$id\'. $ex"); x}
        }
      }

      //category relationship
      lazy val categoryJoin = {
        val postCategoryByIdD = Tables.PostCategory.findBy(_.postId)
        Try[Int](postCategoryByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting PostCategory with post id \'$id\'. $ex"); x}
        }
      }

      //post
      lazy val post = {
        val postByIdD = Tables.Post.findBy(_.id)
        Try[Int](postByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting $Ent with id \'$id\'. $ex"); x}
        }
      }

      //delete components and post.
      for {
        t <- tagJoin
        j <- categoryJoin
        p <- post
      } yield p
  }
}

protected class PostTagJoinRepository extends RepositorySupport {
  //Entity for messages
  //final val Ent: String = "PostMtag"


  def tagsByPostId(id: Long) = db withSession {
    implicit session => val tagsByPostIdC = {
      def q(id: Column[Long]) = for {
        j <- Tables.PostMtag filter(_.postId === id)
        t <- Tables.Mtag filter(_.id === j.mtagId)
      } yield t
      Compiled(q _)
    }
      tagsByPostIdC(id).buildColl[IndexedSeq] map (t => new Tag(t))
  }
}

protected class PostCategoryJoinRepository extends RepositorySupport {
  //Entity for messages
  //final val Ent: String = "PostCategory"

  def categoriesByPostId(id: Long) = db withSession {
    implicit session => val categoriesByPostIdC = {
      def q(id: Column[Long]) = for {
        j <- Tables.PostCategory filter(_.postId === id)
        t <- Tables.Category filter(_.id === j.categoryId)
      } yield t
      Compiled(q _)
    }
      categoriesByPostIdC(id).buildColl[IndexedSeq] map (c => new Category(c))
  }
}


protected class PageTagJoinRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "PageMtag"

  /** remove an Mtag from Page by Page id and Mtag name
    *
    * @return The count of deleted records.
    */
  def removeTagFromPage(pageId: Long, tagName: String): Try[Int] = db withSession {
   implicit session => val q = for {
     mtag <- Tables.Mtag.filter(_.name === tagName)
     j <- Tables.PageMtag if j.pageId === pageId && j.mtagId === mtag.id
   } yield j
   Try{q.delete} match {
     case x @ Success(count: Int) if count == 1 => x
     case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
     case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
   }
  }

  def delete(id: Long): Try[Int] = db withTransaction {
  implicit session => queryHelpers.deletePageMtagJoin(id) match {
    case x @ Success(count: Int) if count == 1 => x
    case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
    case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
  }
}

  /** create based on supplied [[fink.data.Tables.Page]] id and [[fink.data.Tables.Mtag]] id
    *
    * @param pageId to join with Page entity
    * @param tagId to join with Mtag entity
    * @return Id of the [[fink.data.Tables.PageMtag]] table for new join
    */
  def create(pageId: Long, tagId: Long): Try[Long] = db withTransaction {
    implicit session => queryHelpers.createPageMtagJoin(pageId, tagId) match {
        case x @ Failure(ex) => {error(s"Error creating new $Ent with pageId \'$pageId\' and tagId \'$tagId\'."); x}
        case x @ Success(_: Long) => x
      }
  }
}

class TagRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "Mtag"

  /** find Seq[Tag] from the Mtag table
    *
    * @return Seq containing any or all Tag records
    */
  def findAll: IndexedSeq[Tag]  = db withSession {
    implicit session => Tables.Mtag.buildColl[IndexedSeq] map (new Tag(_))
  }

  /** find Option[Tag] based on supplied id
    *
    * @return Option of Tag
    */
  def byId(id: Long) : Option[Tag] = db withSession {
    implicit session =>
      val tagByIdC = Tables.Mtag.findBy(_.id)
      tagByIdC(id).firstOption map(new Tag(_))

    /*
     Using TableQueryExtensionMethods to create a compiled
     query that takes one Column parameter.  The same thing
     could be done like the following:

         def q(id: Column[Long]) = for {
             mtag <- Tables.Mtag if mtag.id is id
           } yield mtag

         val tagByIdC = Compiled(q _)
         tagByIdC(id).firstOption map(new Tag(_))
    */
  }

  /** find Option[Tag] based on supplied name
    *
    * @return Option of Tag
    */
  def byName(name: String) : Option[Tag] = db withSession {
    implicit session => {
      val tagByNameC = {
        def q(name: Column[String]) = for {
          mtag <- Tables.Mtag if mtag.name is name
        } yield mtag
        Compiled(q _)
      }
      tagByNameC(name).firstOption map (new Tag(_))
    }
  }

  /** create based on new unique name
    *
    * @note While only lower-case names should be permitted,
    *       this method does __not__ enforce lower-case naming.
    *       It is expected that such a constraint will be handled
    *       in the service layer.
    *
    * @return Try of the auto-generated pk id of new tag
    */
  def create(name: String): Try[Long] = db withSession {
    implicit session => queryHelpers.createMtag(name) match {
      case x @ Success(_: Long) => x
      case x @ Failure(ex) => {error(s"Error creating new $Ent with name \'$name\'. ${ex}"); x}
    }
  }

  /** update to new unique name
    *
    * Attempt to update the supplied tag so the new name matches the
    * name in the supplied tag.  It is possible that a matching tag can not
    * be found.  It is also possible that the new name is not unique, and thus
    * can not be accepted.
    *
    * @return Try[Int] representing the number of records altered by the update
    */
  def update(mtag: Tag): Try[Int] = db withTransaction {
    implicit session => Try {Tables.Mtag.filter(_.id === mtag.id).update(mtag.toRow)} match {
      case x @ Success(count: Int) if count > 0 => x
      case x @ Success(count: Int) if count == 0 => {warn(emptyUpdateWarning(Ent)); x}
      case x @ Failure(ex) => {error(updateError(Ent, ex)); x}
    }
  }

  /** delete based on id
    *
    * Many tables have a relationship with this one so it is possible that
    * a particular record may not be deleted until its references are released.
    *
    * @param id [[fink.data.Tables.Mtag]] id
    * @return Try[Int] representing the number of records deleted
    */
  def delete(id: Long): Try[Int] = db withTransaction {
    implicit session => Try { Tables.Mtag.filter(_.id === id).delete} match {
      case x @ Success(count: Int) if count == 1 => x
      case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
      case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
    }
  }
}

class CategoryRepository extends RepositorySupport {
  final val Ent: String = "Category"
  def findAll = db withSession {
    implicit session => Tables.Category.buildColl[IndexedSeq] map (new Category(_))
  }

  def byId(id: Long): Option[Category] = db withSession {
    implicit session => val categoryByIdC = Tables.Category.findBy(_.id)
      categoryByIdC(id).firstOption map(new Category(_))
  }

  def byName(name: String): Option[Category] = db withSession {
    implicit session => val categoryByNameC = Tables.Category.findBy(_.name)
      categoryByNameC(name).firstOption map(new Category(_))
  }
  def create(category: Category): Try[Category] = db withTransaction {
    //NOTE: category in two steps because for better db compatibility for 'returning' on insert
    implicit session => val insertedKey = Try[Long] (Tables.Category returning Tables.Category.map(_.id) += category.toRow) match {
        case x: Success[Long] => x
        case x @ Failure(ex) => {error(s"Error creating $Ent with name \'${category.name}\'. $ex"); x}
      }

      insertedKey.flatMap(byId(_) match {
        case Some(x) => Success(x)
        case _ => Failure(wError(s"Unknown error creating $Ent with name \'${category.name}\'"))
      })
  }

  def update(category: Category) = db withTransaction {
    implicit session => Try[Int] {Tables.Category.filter(_.id === category.id).update(category.toRow)} match {
      case x @ Success(count: Int) if count > 0 => x
      case x @ Success(count: Int) if count == 0 => {warn(emptyUpdateWarning(Ent)); x}
      case x @ Failure(ex) => {error(updateError(Ent, ex)); x}
    }
  }

  def delete(id: Long) = db withTransaction {
    implicit session => Try[Int] { Tables.Category.filter(_.id === id).delete} match {
      case x @ Success(count: Int) if count == 1 => x
      case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
      case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
    }
  }
}

class GalleryRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "Gallery"

  protected def load(partGalleries: Seq[Tables.GalleryRow]): Seq[Gallery] = {
    /* Tables.GalleryRow.coverId is Option[Long], i.e. could be absent
     * so we need to handle each option for the cover component.
     */
    partGalleries.map (g => {val cover = g.coverId match {
        case Some(coverId) => imageRepository.byId(coverId)
        case None => None
      }
      new Gallery(g, galleryTagJoinRepository.tagsByGalleryId(g.id),
        galleryImageJoinRepository.imagesByGalleryId(g.id) , cover)})
  }

  def findAll : Seq[Gallery] = db withSession {
    implicit session => load(Tables.Gallery.buildColl[Seq])
  }

  def byId(id: Long) : Option[Gallery] = db withSession {
    implicit session => maybeGetGalleryById(id)
  }

  protected def maybeGetGalleryById(id: Long)(implicit session: Session): Option[Gallery] = {
    val q = Tables.Gallery.findBy(_.id)
    load(q(id).firstOption.toSeq) match {
      case x::nil => Some(x)
      case _ => None
    }
  }

   def byShortlink(shortlink: String) : Option[Gallery] = db withSession {
     implicit session => val galleryByShortlinkC = Tables.Gallery.findBy(_.shortlink)
     load(galleryByShortlinkC(shortlink).firstOption.toSeq) match {
       case x::nil => Some(x)
       case _ => None
     }
   }

  def create(gallery: Gallery): Try[Gallery] = db withTransaction {
    implicit session => val result = Try {
      //make the gallery
        val gId = (Tables.Gallery returning Tables.Gallery.map(_.id)) += gallery.toRow

        //add Tags
        Tables.GalleryMtag ++= gallery.tags map (x => Tables.GalleryMtagRow(IgnoredLong, gId, x.id))

        //add Images
        Tables.GalleryImage ++= gallery.images.zipWithIndex map {case(image, index) =>
          Tables.GalleryImageRow(IgnoredLong, gId, image.id, index.toLong)}

        maybeGetGalleryById(gId).get
        }

      result match {
        case Failure(ex) => error(s"Error creating $Ent. $ex")
        case _ =>
      }
      result
  }

  def update(gallery: Gallery) = db withTransaction {
    implicit session =>
    //gallery
      lazy val uGallery = Tables.Gallery.filter(_.id === gallery.id).update(gallery.toRow)

      lazy val uTags = {//tags

        // must split out difference between supplied (s) and persisted (p)data
        val q = Tables.GalleryMtag.filter(_.galleryId === gallery.id)

        //complement of p \ s, i.e. the items in p not in s must be removed from p
        //records in GalleryMtag for the given gallery id where the mtagId is not found in gallery.tags
        q.filterNot(_.mtagId inSet gallery.tags.map(_.id)).delete

        //intersection, c ∩ p, need not be changed because it contains only join logic.

        //complement of s \ p, i.e. the items in s not in p must be inserted into p
        //insert into GalleryMtag each gallery.tags element not found in GalleryMtag for the given gallery id
        val gtJoin = q.buildColl[Set]
        val newTags = gallery.tags.filterNot(s => gtJoin.exists(_.mtagId == s.id))
        Tables.GalleryMtag ++= newTags map (x => Tables.GalleryMtagRow(IgnoredLong, gallery.id, x.id))
      }

      lazy val uImages = {//images

        // must split out difference between supplied (s) and persisted (p) data
        val q = Tables.GalleryImage.filter(_.galleryId === gallery.id)

        //complement of p \ s, i.e. the items in p not in s must be removed from p
        //records in GalleryImage for the given gallery id where imageId is not found in gallery.images
        q.filterNot(_.imageId inSet gallery.images.map(_.id)).delete

        //intersection, c ∩ p
        val iz = gallery.images.map(_.id).zipWithIndex
        val r = q.filter(_.imageId inSet iz.map{case(imageId, index) => imageId}).buildColl[Set]

        val galleryImageByIdSortU = {
          //Note: move to query helpers?
          def u(id: Column[Long], sort: Column[Long]) = for {
            j <- Tables.GalleryImage if j.id === id && j.sort =!= sort
          } yield j.sort
          Compiled(u _)
        }

        // NOTE:  There is ONE, pair, argument to galleryImageByIdSortU
        for (j <- r) for {
          i <- iz.toMap.get(j.imageId)
          index = i.toLong
        } galleryImageByIdSortU((j.id, index)).update(index)

        //complement of s \ p, i.e. the items in s not in p must be inserted into p
        //insert into GalleryImage each gallery.images element not found in GalleryImage for the given gallery id
        val giJoin = q.buildColl[Set]
        val newImages = iz.filterNot{case (imageId, index) => giJoin.exists(_.imageId == imageId)}
        Tables.GalleryImage ++= newImages map{case (imageId, index) => Tables.GalleryImageRow(IgnoredLong, gallery.id, imageId, index.toLong)}
      }

      (for {
        _ <- Try{uGallery}
        _ <- Try{uTags}
        c <- Try{uImages}
      } yield c) match {
        case x @ Success(_) => x
        case x @ Failure(ex) => {error(updateError(Ent, ex)); x}
      }
  }

  def delete(id: Long): Try[Int] = db withTransaction {
    implicit session =>
    //tag relationship
      lazy val tagJoin = {
        val galleryMTagByIdD = Tables.GalleryMtag.findBy(_.galleryId)
        Try[Int](galleryMTagByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting GalleryMtag with gallery id \'$id\'. $ex"); x}
        }
      }

      //image relationship
      lazy val imageJoin = {
        val galleryImageByIdD = Tables.GalleryImage.findBy(_.galleryId)
        Try[Int](galleryImageByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting GalleryImage with gallery id \'$id\'. $ex"); x}
        }
      }

      //gallery
      lazy val gallery = {
        val galleryByIdD = Tables.Gallery.findBy(_.id)
        Try[Int](galleryByIdD(id).delete) match {
          case x: Success[Int] => x
          case x @ Failure(ex) => {error(s"Error deleting $Ent with id \'$id\'. $ex"); x}
        }
      }

      //delete components and gallery. Rollback on any failure.
      for {
        t <- tagJoin
        j <- imageJoin
        g <- gallery
      } yield g
  }
}

class GalleryTagJoinRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "GalleryMtag"

  /**
   *
   * @param id of a particular gallery
   * @return Seq of [[fink.data.Tag]]
   */
  def tagsByGalleryId(id: Long) = db withSession {
    implicit session =>  val tagsByGalleryIdC = {
      def q(id: Column[Long]) = for {
        j <- Tables.GalleryMtag filter(_.galleryId === id)
        t <- Tables.Mtag filter(_.id === j.mtagId)
      } yield t
      Compiled(q _)
    }
      tagsByGalleryIdC(id).buildColl[IndexedSeq] map(t => new Tag(t))
  }
}

class GalleryImageJoinRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "GalleryImage"

  def imagesByGalleryId(id: Long) = db withSession {
    implicit session => val imagesByGalleryIdC = {
      def q(id: Column[Long]) = for {
        j <- (Tables.GalleryImage filter(_.galleryId === id)).sortBy(_.sort)
        x <- Tables.Image filter(_.id === j.imageId)
      } yield x
      Compiled(q _)
    }
      imagesByGalleryIdC(id).buildColl[IndexedSeq] map(x => new Image(x))
  }
}

class ImageRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "Image"

  /** find Seq[Image] from the Image table
    *
    * @return Seq containing any or all Images
    */
  def findAll : Seq[Image] = db withSession {
    implicit session => Tables.Image.buildColl[Seq] map (new Image(_))
  }

  /** find Image by id
    *
    * @return Optional Image
    */
  def byId(id: Long): Option[Image] = db withSession {
    implicit session => val imageByIdC = Tables.Image.findBy(_.id)
      imageByIdC(id).firstOption map (new Image(_))
  }

  /** find Image by hash
    *
    * @return Optional Image
    */
  def byHash(hash: String) : Option[Image] = db withSession {
    implicit session => val imageByHashC = Tables.Image.findBy(_.hash)
      imageByHashC(hash).firstOption map (new Image(_))
  }

  /** create/persist Image
    *
    * @note id is not needed because it would be ignored anyway
    *
    * @return Try of id of [[fink.data.Tables.Image]]
    */
  def create(date: DateTime, title: String, author: String, hash: String, contentType: String,
             filename: String): Try[Long] = db withTransaction {
    implicit session => Try[Long]{(Tables.Image returning Tables.Image.map(_.id)) +=
    Tables.ImageRow(IgnoredLong, date.toTimestamp, title, author, hash, contentType, filename)} match {
      case x @ Success(_: Long) => x
      case x @ Failure(ex) => {error(s"Error creating new $Ent. ${ex}"); x}
    }
  }

  /** create/persist Image
    *
    * @param image [[fink.data.Image]]
    * @return Try of id of [[fink.data.Tables.Image]]
    */
  def create(image: Image): Try[Long] =
    create(image.date, image.title, image.author, image.hash, image.contentType, image.filename)

  /** update the image
    *
    * @return Try of the count of modified records
    */
  def update(image: Image): Try[Int] = db withTransaction {
    implicit session => Try{Tables.Image.filter(_.id === image.id).update(image.toRow)} match {
      case x @ Success(count: Int) if count > 0 => x
      case x @ Success(count: Int) if count == 0 => {warn(emptyUpdateWarning(Ent)); x}
      case x @ Failure(ex) => {error(updateError(Ent, ex)); x}
    }
  }

  /** naive delete
    */
  def delete(id: Long): Try[Int] = db withTransaction {
    implicit session => Try{(Tables.Image filter(_.id === id)).delete} match {
      case x @ Success(count: Int) if count == 1 => x
      case x @ Success(count: Int) => {warn(wrongCountDeleteWarning(Ent)); x}
      case x @ Failure(ex) => {error(deleteError(Ent, ex)); x}
    }
  }
}

//FIXME the database entity needs to updated with many-to-many rel
class SettingRepository extends RepositorySupport {
  //Entity for messages
  final val Ent: String = "Setting"

  def find: Option[Setting] = db withSession {
    implicit session => Tables.Setting.firstOption map (new Setting(_))
  }

  def byId(id: Long): Option[Setting] = db withSession {
    implicit session =>  val q = Tables.Setting.findBy(_.id)
    q(id).firstOption map(new Setting(_))
  }

  def create(setting: Setting): Try[Setting] = db withTransaction {
    implicit session => val key = Try[Long] {
      Tables.Setting returning Tables.Setting.map(_.id) += setting.toRow
    }
      key match {
        case Failure(ex) => error(s"Error creating $Ent. $ex")
        case _ =>
      }
      key.flatMap(byId(_) match {
        case Some(x) => Success(x)
        case _ => Failure(wError(s"Unknown error creating $Ent."))
      })
  }

  def update(setting: Setting): Try[Int] = db withTransaction {
    implicit session => val r = Try[Int]{Tables.Setting.filter(_.id === setting.id).update(setting.toRow)}
      r match {
        case Failure(ex) => error(updateError(Ent,ex))
        case _ =>
      }
      r
  }
}
