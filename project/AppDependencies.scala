import sbt.Keys.libraryDependencies
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.1.0"
  

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "commons-codec"           % "commons-codec"               % "1.19.0",
    "org.apache.santuario"    % "xmlsec"                      % "3.0.3" exclude("com.fasterxml.woodstox" , "woodstox-core")
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    
  )

  val it: Seq[Nothing] = Seq.empty
}
