package software.ralf.app.platform.metro.compiler

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import software.ralf.app.platform.metro.compiler.runners.AbstractBoxTest
import software.ralf.app.platform.metro.compiler.runners.AbstractFirDiagnosticTest
import software.ralf.app.platform.metro.compiler.runners.AbstractFirDumpTest

fun main() {
  generateTestGroupSuiteWithJUnit5 {
    testGroup(
      testDataRoot = "metro-extensions/contribute/impl-compiler-plugin/src/test/resources",
      testsRoot = "metro-extensions/contribute/impl-compiler-plugin/src/test/java",
    ) {
      testClass<AbstractBoxTest> { model("box") }
      testClass<AbstractFirDiagnosticTest> { model("diagnostics") }
      testClass<AbstractFirDumpTest> { model("dump") }
    }
  }
}
