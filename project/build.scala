import sbt._
import Keys._

import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

import sbtbuildinfo.Plugin._

object ScalatralinkeddataBuild extends Build {
  val Organization = "fink"
  val Name = "fink"
  val Version = "0.2.0-SNAPSHOT"
  val ScalaVersion = "2.10.3"
  val ScalatraVersion = "2.2.0"

  import java.net.URL

  val scalacFlags =  Seq(
    "-deprecation", 
    "-encoding", "UTF-8", 
    "-feature",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    //"-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-all",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"     
  )

  val javacFlags = Seq("-encoding", "UTF-8")

  lazy val project = Project (
    "fink",
    file("."),
    settings = Defaults.defaultSettings ++
    buildInfoSettings ++
    Seq(
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys ++= Seq[BuildInfoKey](
        name, 
        version, 
        scalaVersion, 
        sbtVersion,
        BuildInfoKey.action("dataSourceInfo") { dataSourceInfo } // re-computed each time at compile
        ),
          buildInfoPackage := "fink"
      ) ++ 
    ScalatraPlugin.scalatraWithJRebel ++ 
    //scalateSettings ++
    Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      scalacOptions ++= scalacFlags,
      javacOptions ++= javacFlags,
      resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      scalatraCrossVersion <<= (scalaVersion) {
        case v if v startsWith "2.9." => CrossVersion.Disabled
        case _ => CrossVersion.binary
      },
      libraryDependencies <<= (libraryDependencies, scalatraCrossVersion) {
        (libs, scalatraCV) => libs ++ Seq(
          "org.scala-lang" % "scala-compiler" % ScalaVersion,
          "org.scala-lang" % "scala-reflect" % ScalaVersion,
          "org.scalatra" % "scalatra" % ScalatraVersion cross scalatraCV,
          "org.scalatra" % "scalatra-scalate" % ScalatraVersion cross scalatraCV,
          "org.scalatra" % "scalatra-specs2" % ScalatraVersion % "test" cross scalatraCV,
          "org.scalatra" % "scalatra-json" % ScalatraVersion cross scalatraCV,
          "org.scalatra" % "scalatra-auth" % ScalatraVersion cross scalatraCV,
          "org.scalatra" %% "scalatra-swagger" % ScalatraVersion cross scalatraCV,
          "org.json4s"   %% "json4s-jackson" % "3.1.0",
          "com.github.nscala-time" %% "nscala-time" % "1.0.0",
          "com.typesafe.slick" % "slick_2.10" % "2.0.1",
          "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
          "c3p0" % "c3p0" % "0.9.1.2",
          "org.fusesource.scalamd" %% "scalamd" % "1.6",
          "commons-io" % "commons-io" % "2.0.1",
          "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
          "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1",
          "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
          "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
        )
      },
      slick <<= slickCodeGenTask, // register manual sbt command
      sourceGenerators in Compile <+= slickCodeGenTask // register automatic code generation on every compile, uncomment if you want this
      //and add comma above
    )
  )

  val scalatraCrossVersion = SettingKey[CrossVersion]("scalatra-cross-version", "cross build strategy for Scalatra")

  // Define dataSource information for dataSource options
  val dataSourceInfo: Map[String, Map[Symbol, String]] = Map("default" ->
    Map('url -> "jdbc:postgresql://localhost/fink-db",
      'jdbcDriver -> "org.postgresql.Driver",
      'slickDriver -> "scala.slick.driver.PostgresDriver",
      'user -> "development",
      'password -> "foo"),
    "staging" -> 
    Map('url -> "jdbc:postgresql://localhost/fink-db-staging",
      'jdbcDriver -> "org.postgresql.Driver",
      'slickDriver -> "scala.slick.driver.PostgresDriver",
      'user -> "staging",
      'password -> "foo"))


  // code generation task
  lazy val slick = TaskKey[Seq[File]]("gen-tables")

  // select the correct dataSource based on the value of the cpds environment variable
  // default to "default"
  lazy val selectedSource = scala.util.Properties.envOrElse("cpds", "default")
  println(s"Selected data source designated with \'cpds\' variable is \'$selectedSource\'.")

  lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
    val maybeFileName: Option[Seq[sbt.File]] = for { 
      selectedSourceInfo <- dataSourceInfo get selectedSource
      url <- selectedSourceInfo get 'url
      user <- selectedSourceInfo get 'user
      password <- selectedSourceInfo get 'password  //may not work if password is not set
      fullURL = s"$url?user=$user&password=$password&"
      jdbcDriver <- selectedSourceInfo get 'jdbcDriver
      slickDriver <- selectedSourceInfo get 'slickDriver
      pkg = "fink.data"
      outputDir = (dir / "main").getPath // place generated files in sbt's managed sources folder
      empty = toError(r.run("scala.slick.model.codegen.SourceCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, fullURL, outputDir, pkg), s.log))
      //note: regex replace on pkg from . to / 
      fname = outputDir + "/" + ("\\.".r replaceAllIn(pkg, "/")) + "/Tables.scala"
      } yield Seq(file(fname))
    maybeFileName match {
      case Some(s) => s
      case _ => {new Exception("slickCodeGenTask failure"); Seq()}
    }
    }
  }
