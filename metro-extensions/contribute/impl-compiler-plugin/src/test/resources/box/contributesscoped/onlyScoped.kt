package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

@Inject
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass : Scoped

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

fun box(): String {
  val graph = createGraph<GraphInterface>()
  val scoped = graph.allScoped.single()
  return if (scoped is TestClass) "OK" else "FAIL: expected TestClass but got $scoped"
}
