lazy val root = (project in file(".")).settings(
  organization := "com.tresata",
  name := "scopt-auto",
  version := "0.1.0-SNAPSHOT",
  licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"),  
  scalaVersion := "2.12.15",
  crossScalaVersions := Seq("2.12.15", "2.13.5"),
  //scalacOptions += "-Xlog-implicits",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "compile",
    "com.github.scopt" %% "scopt" % "4.0.1" % "compile",
    "com.chuusai" %% "shapeless" % "2.3.3" % "compile",
    "org.scalatest" %% "scalatest-funspec" % "3.2.10" % "test"
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  Test / publishArtifact := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at "https://server02.tresata.com:8084/artifactory/oss-libs-snapshot-local")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".m2" / "credentials_sonatype"),
  credentials += Credentials(Path.userHome / ".m2" / "credentials_artifactory"),
  Global / useGpgPinentry := true,
  pomExtra := (
    <url>https://github.com/tresata/scopt-auto</url>
    <scm>
      <url>git@github.com:tresata/scopt-auto.git</url>
      <connection>scm:git:git@github.com:tresata/scopt-auto.git</connection>
    </scm>
    <developers>
      <developer>
        <id>koertkuipers</id>
        <name>Koert Kuipers</name>
        <url>https://github.com/koertkuipers</url>
      </developer>
    </developers>)
)
