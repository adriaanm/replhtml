import java.io.File
import play.api.{Play, GlobalSettings}
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.mvc.Results._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{Action, WebSocket, Handler, RequestHeader}
import play.core.server.NettyServer
import play.core.StaticApplication

/** Expose the REPL as a web socket, serve static HTML slides,
  * and trigger regeneration of those slides with keydown when
  * the source markdown file changes.
  */
object Global extends GlobalSettings {
  private val appProvider = new StaticApplication(new File("."))
  private val config      = appProvider.application.configuration
  private val keydownRoot = config.getString("keydown.root")
  private val port        = config.getInt("http.port").getOrElse(8081)
  private val keyDown     = new Keydown(new File(keydownRoot.get))

  def main(args: Array[String]) {
    ReplMain
    val server = new play.core.server.NettyServer(appProvider, port)
    println("Press any key to stop.")
    readLine()
    shutdown(server) // TODO SBT doesn't return the the prompt; why not?
  }

  private def shutdown(server: NettyServer) {
    server.stop()
    Play.stop()
    println("stopped")
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val handler = request.path match {
      case "/socket/repl" => simpleWebSocket(ReplMain.interpret)
      case "/"            => Action(Redirect("slides.html"))
      case path           => keydownStaticContent(path)
    }
    Some(handler)
  }

  private def keydownStaticContent(path: String) = Action {
    keyDown.regenerateSlidesIfNeeded()
    val f = (keyDown / path)
    if (f.exists) Ok.sendFile(f, inline = true) else NotFound
  }

  private def simpleWebSocket[A: FrameFormatter](f: A => A) = WebSocket.using[A] {
    request =>
      val (outEnumerator, outChannel) = Concurrent.broadcast[A]
      val in = Iteratee.foreach[A] {
        msg => outChannel.push(f(msg))
      }
      (in, outEnumerator)
  }
}
