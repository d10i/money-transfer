organization  := "com.moneytransfer"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"                %% "spray-can"        % sprayV,
    "io.spray"                %% "spray-routing"    % sprayV,
    "com.typesafe.akka"       %% "akka-actor"       % akkaV,
    "com.typesafe.akka"       %% "akka-slf4j"       % akkaV,
    "com.typesafe.akka"       %% "akka-testkit"     % akkaV      % "test",
    "io.spray"                %% "spray-testkit"    % sprayV     % "test",
    "org.specs2"              %% "specs2"           % "2.4.15"   % "test",
    "org.json4s"              %% "json4s-native"    % "3.3.0",
    "ch.qos.logback"          %  "logback-classic"  % "1.1.3"
  )
}

Revolver.settings