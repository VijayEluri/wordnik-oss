import com.wordnik.sbt.DistPlugin
import sbt._
import Keys._
import xml.Group
import sbtrelease.ReleasePlugin._

object WordnikOssProject extends Build {
  
  import Resolvers._
  import Dependencies._

  val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  val publishSettings: Seq[Setting[_]] = Seq(
    publishTo <<= (version) { version: String =>
      if (version.trim endsWith "SNAPSHOT") Some(sonatypeNexusSnapshots) else Some(sonatypeNexusStaging)
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    homepage := Some(new URL("https://github.com/wordnik/wordnik-oss")),
    startYear := Some(2009),
    licenses := Seq(("Apache License 2.0", new URL("http://www.apache.org/licenses/LICENSE-2.0.html"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
      <scm>
        <connection>scm:git:git@github.com:wordnik/wordnik-oss.git</connection>
        <developerConnection>scm:git:git@github.com:wordnik/wordnik-oss.git</developerConnection>
        <url>https://github.com/wordnik/wordnik-oss</url>
      </scm>
      <developers>
        <developer>
          <id>fehguy</id>
          <name>Tony Tam</name>
          <email>fehguy@gmail.com</email>
        </developer>
      </developers>
      <issueManagement>
        <system>github</system>
        <url>https://github.com/wordnik/wordnik-oss/issues</url>
      </issueManagement>
      <mailingLists>
        <mailingList>
          <name>wordnik-api</name>
          <archive>https://groups.google.com/forum/#!forum/wordnik-api</archive>
        </mailingList>
      </mailingLists>
    )}
  )

  def versionSpecificSourcesIn(c: Configuration) =
    unmanagedSourceDirectories in c <+= (scalaVersion, sourceDirectory in c) {
      case (v, dir) if v startsWith "2.9" => dir / "scala_2.9"
      case (v, dir) if v startsWith "2.10" => dir / "scala_2.10"
      case (v, dir) if v startsWith "2.11" => dir / "scala_2.11"
    }

  val projectSettings = Defaults.defaultSettings ++ releaseSettings ++ publishSettings ++ Seq(
    organization := "com.wordnik",
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.10.0", "2.10.0", "2.10.4", "2.11.1"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xcheckinit", "-encoding", "utf8"),
    crossVersion := CrossVersion.binary,
    javacOptions in compile ++= Seq("-target", "1.6", "-source", "1.6", "-Xlint:deprecation"),
    manifestSetting,
    scalacOptions in Compile <++= scalaVersion map ({
      case v if v startsWith "2.10" => Seq("-feature")
      case _ => Seq.empty
    }),
    parallelExecution in Test := false,
    libraryDependencies ++= slf4j,
    libraryDependencies ++= testDependencies,
    resolvers <++= scalaVersion {
      case v if v.startsWith("2.9") => Seq(Classpaths.typesafeReleases)
      case _ => Seq.empty
    }
  )

  lazy val root = 
    (Project(id = "wordnik-oss", base = file("."), settings = projectSettings ) 
      dependsOn (commonUtils, mongoUtils, mongoAdminUtils)
      aggregate (commonUtils, mongoUtils, mongoAdminUtils))

  lazy val commonUtils = Project(
    id = "common-utils",
    base = file("modules/common-utils"),
    settings = projectSettings ++ Seq(
      libraryDependencies ++= Seq(commonsLang, metricsCore),
      libraryDependencies <++= scalaVersion({
        case v if v startsWith "2.9" => Seq(akka)
        case _                       => Seq.empty
      }),
      versionSpecificSourcesIn(Compile)
    )
  )

  lazy val mongoUtils = Project(
    id = "mongo-utils",
    base = file("modules/mongo-utils"),
    settings = projectSettings ++ Seq(
      libraryDependencies ++= Seq(mongoJava, jackonsJaxRs)
    )
  )

  lazy val mongoAdminUtils = Project(
    id = "mongo-admin-utils",
    base = file("modules/mongo-admin-utils"),
    settings = projectSettings ++ DistPlugin.distSettings
  ) dependsOn(mongoUtils % "compile;test->test")
}
