import sbt._
import Keys._

object ReplHtmlBuild extends Build {
  val mySettings = Defaults.defaultSettings ++ Seq(
    organization := "ch.epfl.lamp",
    name         := "replhtml",
    version      := "1.1",
    scalaVersion := "2.10.1",
    libraryDependencies <<= (scalaVersion)(sv =>
      Seq("compiler").map(x => "org.scala-lang" % ("scala-" + x) % sv)
    ),
    libraryDependencies += "play" %% "play" % "2.1.1",
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  val setupReplClassPath = TaskKey[Unit]("setup-repl-classpath", "Set up the repl server's classpath based on our dependencies.")

  lazy val project = Project (
    "replhtml",
    file ("."),
    settings = mySettings ++ Seq(
      setupReplClassPath <<= (dependencyClasspath in Compile) map {cp =>
        val cpStr = cp map { case Attributed(str) => str} mkString(System.getProperty("path.separator"))
        println("Repl will use classpath "+ cpStr)
        System.setProperty("replhtml.class.path", cpStr)
      },
      run in Compile <<= (run in Compile).dependsOn(setupReplClassPath)
    )
  )
}
