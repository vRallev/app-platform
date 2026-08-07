package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

@Inject
class ScopedDependency {
  fun value(): String = "dependency"
}

@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass private constructor(
  val dependency: ScopedDependency,
  val marker: String,
) : SuperType, Scoped {
  @Inject constructor(dependency: ScopedDependency) : this(dependency, "injected")
}

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val superTypeInstance: SuperType

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

fun box(): String {
  val provider =
    TestClass.ScopedContribution::class.java.declaredMethods.singleOrNull {
      it.name == "provideTestClass"
    }
  if (provider != null) {
    return "FAIL: expected generated provider to be skipped"
  }

  val graph = createGraph<GraphInterface>()
  val scoped = graph.allScoped.single()
  if (graph.superTypeInstance !is TestClass) {
    return "FAIL: expected TestClass super type binding but got ${graph.superTypeInstance}"
  }
  if (scoped !is TestClass) {
    return "FAIL: expected TestClass scoped binding but got $scoped"
  }
  if (graph.superTypeInstance !== scoped) {
    return "FAIL: expected shared singleton instance"
  }

  return if (scoped.dependency.value() == "dependency" && scoped.marker == "injected") {
    "OK"
  } else {
    "FAIL: dependency was not injected through secondary constructor"
  }
}
