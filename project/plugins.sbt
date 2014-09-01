// https://github.com/sbt/sbt-buildinfo
// Generates Scala source from your build definitions.
// Being used to define dataSource information in one place, 
// the build.scala file, and use it where needed:
// 1. Required in the build.scala file to perform slick codegen from a live dataSource
// 2. Required for Fink to connect to a datasource
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")

// https://github.com/scalate/xsbt-scalate-generate
// Integration for SBT that lets you generate sources for your Scalate templates
// and precompile them as part of the normal compilation process.
addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

// https://github.com/scalatra/scalatra-sbt
// This plugin adds a browse task, to open the current project in a browser. 
// It depends on xsbt-web-plugin so you don't need to specify that explicitly.
addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.3.2")
