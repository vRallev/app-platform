package software.ralf.app.platform.metro.compiler.runners

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.ir.AbstractFirLightTreeJvmIrTextTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import software.ralf.app.platform.metro.compiler.services.configureMetroImports
import software.ralf.app.platform.metro.compiler.services.configurePlugin

open class AbstractFirDumpTest : AbstractFirLightTreeJvmIrTextTest() {
  override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
    return EnvironmentBasedStandardLibrariesPathProvider
  }

  override fun configure(builder: TestConfigurationBuilder) {
    super.configure(builder)

    with(builder) {
      configurePlugin()
      configureMetroImports()

      defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JVM_TARGET.with(JvmTarget.JVM_11)
        +ConfigurationDirectives.WITH_STDLIB
        +JvmEnvironmentConfigurationDirectives.FULL_JDK
        +FirDiagnosticsDirectives.FIR_DUMP
        +FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
        +CodegenTestDirectives.IGNORE_DEXING
        -CodegenTestDirectives.DUMP_IR
        -CodegenTestDirectives.DUMP_KT_IR
      }
    }
  }
}
