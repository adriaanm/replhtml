import scala.util.matching.Regex
import scala.util.Try

object ReplMain {

  import scala.tools.nsc._
  import scala.tools.nsc.interpreter._

  val cmd = new CommandLine(Nil, println)

  import cmd.settings

  settings.classpath.value = System.getProperty("replhtml.class.path")
  settings.lint.value = true
  settings.feature.value = true

  val interpreter = new IMain(settings)
  val completion  = new JLineCompletion(interpreter)

  def interpret(data: String): String = {
    // TODO: use json
    implicit class RContext(sc: StringContext) {
      def rx = new Regex(sc.parts.mkString(""), sc.parts.tail.map(_ => "x"): _*)
    }
    object I {
      def unapply(x: String): Option[Int] = scala.util.Try(x.toInt).toOption
    }
    data.split(":", 2).toSeq match {
      case Seq(rx"""complete@(\d*)${I(pos)}[\]]?""", source) =>
        "<completion>:" + pos + "\n" + {
          lazy val tokens = source.substring(0, pos).split( """[\ \,\;\(\)\{\}]""") // could tokenize on client
          if (pos <= source.length && tokens.nonEmpty)
            completion.topLevelFor(Parsed.dotted(tokens.last, pos) withVerbosity 4).mkString("\n")
          else ""
        }

      case Seq("run", source) =>
        var commandResult: Any = null
        val s = util.stringFromStream {
          ostream =>
            Console.withOut(ostream) {
              if (source.startsWith(":")) {
                val iloop = new ILoop()
                iloop.intp = interpreter
                iloop command(source) match {
                  case result =>
                    commandResult = result
                    println("<done:success>")
                }
              } else interpreter.interpret(source) match {
                case IR.Error      => println("<done:error>")
                case IR.Success    => println("<done:success>")
                case IR.Incomplete => println("<done:incomplete>")
              }
            }
        }
        println(commandResult)
        s
    }
  }
}
