import sbt.Keys.libraryDependencies
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "commons-codec"           % "commons-codec"               % "1.20.0",
    "org.apache.santuario"    % "xmlsec"                      % "4.0.4" exclude("com.fasterxml.woodstox" , "woodstox-core"),
    "org.json4s"              %% "json4s-jackson"             % "4.0.7"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    
  )

  val it: Seq[Nothing] = Seq.empty
}
