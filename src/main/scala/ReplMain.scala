import scala.util.matching.Regex
import java.io.File
import play.api.libs.json.Json
import play.api.mvc.{WebSocket, Handler, RequestHeader, Action}
import play.api.mvc.Results._
import play.api.GlobalSettings
import play.core.StaticApplication
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import scala.util.Try

object ReplMain {

  import scala.tools.nsc._
  import scala.tools.nsc.interpreter._

  val cmd = new CommandLine(Nil, println)
  import cmd.settings
  settings.classpath.value = System.getProperty("replhtml.class.path")

  val interpreter = new IMain(settings)
  val completion = new JLineCompletion(interpreter)
  // interpreter.bind("servlet", "ch.epfl.lamp.replhtml.ReplServlet", ReplServlet.this) }
  // interpreter.unleash()

  def interpret(data: String): String = {
    // TODO: use json
    implicit class RContext(sc: StringContext) {
      def rx = new Regex(sc.parts.mkString(""), sc.parts.tail.map(_ => "x"): _*)
    }
    object I { def unapply(x: String): Option[Int] = scala.util.Try { x.toInt } toOption }
    data.split(":", 2).toSeq match {
      case Seq(rx"""complete@(\d*)${I(pos)}[\]]?""", source) =>
        "<completion>:" + pos + "\n" + {
          lazy val tokens = source.substring(0, pos).split("""[\ \,\;\(\)\{\}]""") // could tokenize on client
          if (pos <= source.length && tokens.nonEmpty)
            completion.topLevelFor(Parsed.dotted(tokens.last, pos) withVerbosity 4).mkString("\n")
          else ""
        }

      case Seq("run", source) =>
        util.stringFromStream { ostream =>
          Console.withOut(ostream) {
            interpreter.interpret(source) match {
              case IR.Error => println("<done:error>")
              case IR.Success => println("<done:success>")
              case IR.Incomplete => println("<done:incomplete>")
            }
          }
        }
    }
  }
}
