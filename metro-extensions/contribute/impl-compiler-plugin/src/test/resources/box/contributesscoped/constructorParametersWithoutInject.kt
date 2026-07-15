package com.test

import dev.zacsweers.metro.BindingContainer
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

class ScopedDependency {
  fun value(): String = "dependency"
}

@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass(
  val dependency: ScopedDependency,
) : SuperType, Scoped

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val superTypeInstance: SuperType

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>

  @Provides fun scopedDependency(): ScopedDependency = ScopedDependency()
}

fun box(): String {
  if (
    TestClass.ScopedContribution::class.java.getAnnotation(BindingContainer::class.java) == null
  ) {
    return "FAIL: expected ScopedContribution to be a BindingContainer"
  }
  if (
    TestClass.ScopedContribution::class.java.declaredMethods.any { it.name == "provideTestClass" }
  ) {
    return "FAIL: expected provider to be moved off the ScopedContribution interface"
  }
  if (
    TestClass.ScopedContribution.Companion::class.java.declaredMethods.none {
      it.name == "provideTestClass"
    }
  ) {
    return "FAIL: expected provider on ScopedContribution companion"
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
    return "FAIL: expected scoped provider to share the singleton instance"
  }
  if (scoped.dependency.value() != "dependency") {
    return "FAIL: dependency was not injected"
  }

  return "OK"
}
