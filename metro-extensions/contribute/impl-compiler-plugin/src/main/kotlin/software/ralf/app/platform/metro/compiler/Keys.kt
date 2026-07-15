package software.ralf.app.platform.metro.compiler

import org.jetbrains.kotlin.GeneratedDeclarationKey

internal object Keys {
  data object ContributesRendererGeneratorKey : GeneratedDeclarationKey() {
    override fun toString(): String = "ContributesRendererGenerator"
  }

  data object ContributesRobotGeneratorKey : GeneratedDeclarationKey() {
    override fun toString(): String = "ContributesRobotGenerator"
  }

  data object ContributesScopedGeneratorKey : GeneratedDeclarationKey() {
    override fun toString(): String = "ContributesScopedGenerator"
  }
}
