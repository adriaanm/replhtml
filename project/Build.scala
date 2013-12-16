import sbt._
import Keys._

object ReplHtmlBuild extends Build {
  val mySettings = Defaults.defaultSettings ++ Seq(
    organization := "ch.epfl.lamp",
    name         := "replhtml",
    version      := "1.1-SNAPSHOT",
    scalaVersion := "2.10.3",
    libraryDependencies += "org.scala-lang" % "scala-compiler"  % scalaVersion.value,
    libraryDependencies += "play" %% "play" % "2.1.5",
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  val setupReplClassPath = TaskKey[Unit]("setup-repl-classpath", "Set up the repl server's classpath based on our dependencies.")

  lazy val repl = Project("repl", file("repl")).settings(
    scalaVersion := "2.11.0-M7",
    organization := "ch.epfl.lamp",
    name         := "repl",
    version      := "1.0-SNAPSHOT",
    libraryDependencies += "org.scala-lang" % "scala-compiler"  % scalaVersion.value
  )

  lazy val project = Project (
    "replhtml",
    file ("."),
    settings = mySettings ++ Seq(
      libraryDependencies += "ch.epfl.lamp" % s"repl_${(scalaBinaryVersion in repl).value}" % s"${(version in repl).value}",
      setupReplClassPath := {
        val cpStr = (dependencyClasspath in Compile in repl).value map { case Attributed(str) => str} mkString(System.getProperty("path.separator"))
        println("Repl will use classpath "+ cpStr)
        System.setProperty("replhtml.class.path", cpStr)
      },
      run in Compile <<= (run in Compile).dependsOn(setupReplClassPath)
    )
  )
}
