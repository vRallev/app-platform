package software.ralf.app.platform.metro.compiler.services

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isJavaFile

class KotlinTestImportsPreprocessor(testServices: TestServices) :
  ReversibleSourceFilePreprocessor(testServices) {

  private val additionalImports: Set<String> = setOf("kotlin.test.*")

  private val additionalImportsString: String by lazy {
    additionalImports.sorted().joinToString(separator = "\n") { "import $it" }
  }

  override fun process(file: TestFile, content: String): String {
    if (file.isAdditional) return content
    if (file.isJavaFile) return content

    val lines = content.lines().toMutableList()
    when (val packageIndex = lines.indexOfFirst { it.startsWith("package ") }) {
      -1 ->
        when (val nonBlankIndex = lines.indexOfFirst { it.isNotBlank() }) {
          -1 -> lines.add(0, additionalImportsString)
          else -> lines.add(nonBlankIndex, additionalImportsString)
        }
      else -> lines.add(packageIndex + 1, additionalImportsString)
    }
    return lines.joinToString(separator = "\n")
  }

  override fun revert(file: TestFile, actualContent: String): String {
    if (file.isAdditional) return actualContent
    if (file.isJavaFile) return actualContent
    return actualContent.replace(additionalImportsString + "\n", "")
  }
}
