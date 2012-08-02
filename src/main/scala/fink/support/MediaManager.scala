package fink.support

import scala.collection.JavaConversions._

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern

import org.apache.commons.fileupload.FileItem
import org.apache.commons.io.IOUtils
import fink.data._

import javax.imageio.ImageIO
import java.security.MessageDigest

// Image specifications
sealed abstract case class ImageSpec(name: String)
case class FullImageSpec(override val name: String) extends ImageSpec(name)
case class KeepRatioImageSpec(override val name: String, max: Int) extends ImageSpec(name)
case class SquareImageSpec(override val name: String, width: Int) extends ImageSpec(name)

object MediaManager {

  def baseDirectory = Config.mediaDirectory

  val specs = Array(
    FullImageSpec("full"),
    KeepRatioImageSpec("medium", 300),
    SquareImageSpec("thumb", 100),
    KeepRatioImageSpec("big", 700)
  )

  protected def checkDirectory(dir: String) = {
    val target = new File(dir)
    if (!target.exists) target.mkdirs
  }

  // TODO use a more sophisticated method to check for an image
  protected def isImage(item: FileItem, name: String, ext: String): Boolean = true

  def processUpload(item: FileItem): Option[String] = {
    checkDirectory(baseDirectory)
    
    val m = Pattern.compile("(.*)\\.(.*)$").matcher(item.getName)
    if (!m.matches) return None

    val (name, ext) = (m.group(1), m.group(2))
    
    if (isImage(item, name, ext)) {
      getFreeFilename(baseDirectory, name, ext) match {
        case Some(hash) =>
          specs.foreach(spec => processImage(spec, item.getInputStream, hash, ext))
          return Some(hash)
        case None =>
          return None
      }
    }
    None
  }

  protected def processImage(spec: ImageSpec, upload: InputStream, hash: String, ext: String): File = {
    spec match {
      case FullImageSpec(sid) => {
        val target = new File("%s/%s-%s.%s".format(baseDirectory, hash, sid, ext))
        IOUtils.copy(upload, new FileOutputStream(target))
        return target
      }
      case KeepRatioImageSpec(sid, max) => {
        val target = new File("%s/%s-%s.%s".format(baseDirectory, hash, sid, ext))
        scaleImage(upload, new FileOutputStream(target), max)
        return target
      }
      case SquareImageSpec(sid, width) => {
        val target = new File("%s/%s-%s.%s".format(baseDirectory, hash, sid, ext))
        createSquareImage(upload, new FileOutputStream(target), width)
        return target
      }
    }
  }

  /**
   * Returns a free filename. TODO thread-safety
   */
  protected def getFreeFilename(base: String, name: String, ext: String): Option[String] = {
    var hash = md5(java.util.UUID.randomUUID().toString)
    var temp = new File(base + "/" + hash + "." + ext)

    var count = 1
    while (temp.exists) {
      hash = md5(java.util.UUID.randomUUID().toString)
      temp = new File(base + "/" + hash + "." + ext)
      count += 1

      // safety first =)
      if (count == 1000) return None
    }

    return Some(hash)
  }

  protected def scaleImage(in: InputStream, out: OutputStream, largestDimension: Int): Boolean = {
    try {
      val inImage = ImageIO.read(in)
      var width = inImage.getWidth().toFloat
      var height = inImage.getHeight().toFloat

      if (inImage.getWidth > largestDimension && inImage.getWidth >= inImage.getHeight) {
        val ratio = largestDimension.toFloat / inImage.getWidth().toFloat
        width *= ratio
        height *= ratio
      } else if (inImage.getHeight > largestDimension && inImage.getHeight >= inImage.getWidth) {
        val ratio = largestDimension.toFloat / inImage.getHeight().toFloat
        width *= ratio
        height *= ratio
      }

      val outImage = new BufferedImage(width.toInt, height.toInt, BufferedImage.TYPE_INT_RGB);
      outImage.createGraphics().drawImage(
        inImage.getScaledInstance(width.toInt, height.toInt, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
      ImageIO.write(outImage, "jpg", out);
      return true;
    } catch {
      case e: IOException => {
        e.printStackTrace();
        return false;
      }
    }
  }

  protected def createSquareImage(inFile: InputStream, outFile: OutputStream, width: Int): Boolean = {
    try {
      val img = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
      img.createGraphics().drawImage(
        ImageIO.read(inFile).getScaledInstance(width, width, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
      ImageIO.write(img, "jpg", outFile);
      return true;
    } catch {
      case e: IOException => {
        e.printStackTrace();
        return false;
      }
    }
  }
  
  def md5(s : String) : String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(s.getBytes())
    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }
}
