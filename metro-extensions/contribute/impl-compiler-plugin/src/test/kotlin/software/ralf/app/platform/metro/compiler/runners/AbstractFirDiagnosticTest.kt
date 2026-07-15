package software.ralf.app.platform.metro.compiler.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.runners.AbstractFirPhasedDiagnosticTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.TestPhase
import software.ralf.app.platform.metro.compiler.services.configureMetroImports
import software.ralf.app.platform.metro.compiler.services.configurePlugin

open class AbstractFirDiagnosticTest : AbstractFirPhasedDiagnosticTest(FirParser.LightTree) {
  override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
    return EnvironmentBasedStandardLibrariesPathProvider
  }

  override fun configure(builder: TestConfigurationBuilder) =
    with(builder) {
      super.configure(builder)

      defaultDirectives {
        +FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
        +JvmEnvironmentConfigurationDirectives.FULL_JDK
        +CodegenTestDirectives.IGNORE_DEXING
        TestPhaseDirectives.RUN_PIPELINE_TILL.with(TestPhase.FRONTEND)
      }

      configurePlugin()
      configureMetroImports()
    }
}
