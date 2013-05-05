import java.io.File
import scala.util.control.NonFatal
import scala.util.Try

/**
 * @see https://github.com/infews/keydown
 */
class Keydown(val root: File) {
  def /(path: String) = new File(root, path)

  def regenerateSlidesIfNeeded() {
    val sources = Seq(this / "slides.md") ++ (Seq("css", "js", "images").flatMap(p => (this / p).listFiles()))
    val sourceModified = sources.map(_.lastModified()).max
    val slidesHtml = this / "slides.html"
    val outputModified = Try(slidesHtml.lastModified()).getOrElse(0L)
    if (sourceModified > outputModified) {
      val process = sys.process.Process(Seq("keydown", "slides", "slides.md"), root)
      try {
        println(process.!!)
        slidesHtml.setLastModified(System.currentTimeMillis()) // in case just CSS changed.
      } catch {
        case NonFatal(e) => println(e.getMessage)
      }
    }
  }
}
