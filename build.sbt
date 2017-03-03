import com.typesafe.sbt.SbtPgp.pgpPassphrase
import com.typesafe.sbt.pgp.PgpKeys.{publishSigned, useGpg}
import java.util.Properties

import sbtrelease.ReleasePlugin.autoImport.releaseIgnoreUntrackedFiles

lazy val currentVersion = SettingKey[String]("currentVersion")
lazy val nd4jVersion = SettingKey[String]("nd4jVersion")
lazy val publishSomeThing = sys.props.getOrElse("repoType", default = "local").toLowerCase match {
  case repoType if repoType.contains("local-nexus") => publisNexus
  case repoType if repoType.contains("local-jfrog") => publishJfrog
  case repoType if repoType.contains("bintray-jfrog") => publishBintray
  case repoType if repoType.contains("sonatype-nexus") => publishSonatype
  case _ => publishLocalLocal
}

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  name := "nd4s",
  //  version := sys.props.getOrElse("currentVersion", default = "0.7.2-SNAPSHOT"),
  organization := "org.nd4j",
  resolvers += Resolver.mavenLocal,
  nd4jVersion := sys.props.getOrElse("nd4jVersion", default = "0.7.2"),
  libraryDependencies ++= Seq(
    "com.nativelibs4java" %% "scalaxy-loops" % "0.3.4",
    "org.nd4j" % "nd4j-api" % nd4jVersion.value,
    "org.nd4j" % "nd4j-native-platform" % nd4jVersion.value % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test,
    "ch.qos.logback" % "logback-classic" % "1.1.7" % Test,
    "org.scalacheck" %% "scalacheck" % "1.12.5" % Test,
    "org.scalanlp" %% "breeze" % "0.12" % Test,
    "com.github.julien-truffaut" %% "monocle-core" % "1.2.0" % Test
  ),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <url>http://nd4j.org/</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <connection>scm:git@github.com:SkymindIO/deeplearning4j.git</connection>
        <developerConnection>scm:git:git@github.com:SkymindIO/deeplearning4j.git</developerConnection>
        <url>git@github.com:deeplearning4j/deeplearning4j.git</url>
        <tag>HEAD</tag>
      </scm>
      <developers>
        <developer>
          <id>agibsonccc</id>
          <name>Adam Gibson</name>
          <email>adam@skymind.io</email>
        </developer>
        <developer>
          <id>taisukeoe</id>
          <name>Taisuke Oe</name>
          <email>oeuia.t@gmail.com</email>
        </developer>
      </developers>
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  initialCommands in console := "import org.nd4j.linalg.factory.Nd4j; import org.nd4s.Implicits._"
)

import ReleaseTransformations._

lazy val publisNexus = Seq(
  externalResolvers += "Local Sonatype OSS Snapshots" at "http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8088/nexus/content/repositories/snapshots/",
  publishTo := {
    val nexus = "http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8088/nexus/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val publishJfrog = Seq(
  externalResolvers += "Local JFrog OSS Snapshots" at "http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8081/artifactory/libs-snapshot/",
  publishTo := {
    val jfrog = "http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8081/artifactory/"
    if (isSnapshot.value)
      Some("snapshots" at jfrog + "libs-snapshot-local")
    else
      Some("releases" at jfrog + "libs-release-local")
  }
)

lazy val publishBintray = Seq(
  externalResolvers += "JFrog OSS Snapshots" at "https://oss.jfrog.org/artifactory/libs-snapshot/",
  publishTo := {
    val jfrog = "https://oss.jfrog.org/artifactory/"
    if (isSnapshot.value)
      Some("snapshots" at jfrog + "oss-snapshot-local")
    else
      Some("releases" at jfrog + "oss-release-local")
  }
)

lazy val publishSonatype = Seq(
  externalResolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val publishLocalLocal = Seq(
  publish := {},
  publishLocal := {}
)

lazy val releaseProcessCustom = Seq(
  useGpg := true,
  releasePublishArtifactsAction := publishSigned.value,
  pgpPassphrase := Some(passphraseGpg.value.toCharArray),
  releaseCrossBuild := true,
  releaseIgnoreUntrackedFiles := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion //,
    //pushChanges
  ),
  releaseTagName := s"nd4s-${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}-intropro"
)

lazy val root = (project in file(".")).settings(
  commonSettings,
  publishSomeThing,
  releaseProcessCustom
)


/**   This particular part required to Automate Passphrase Entry for this purpose we need to store untracked file "gpg.properties" into project base directory.
  *   Value in file:  gpgpassphrase = some_value
  */
val passphraseGpg = settingKey[String]("The GPG password value")
val appProperties = settingKey[Properties]("The application properties")
appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("gpg.properties"))
  prop
}
passphraseGpg := appProperties.value.getProperty("gpgpassphrase")

