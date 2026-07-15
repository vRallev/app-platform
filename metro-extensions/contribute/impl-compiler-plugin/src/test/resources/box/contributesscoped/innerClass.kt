package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

interface TestClass {
  @Inject
  @SingleIn(AppScope::class)
  @ContributesScoped(AppScope::class)
  class Inner : SuperType, Scoped
}

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val superTypeInstance: SuperType

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

fun box(): String {
  val graph = createGraph<GraphInterface>()
  val scoped = graph.allScoped.single()
  if (graph.superTypeInstance !is TestClass.Inner) {
    return "FAIL: expected TestClass.Inner super type binding but got ${graph.superTypeInstance}"
  }
  if (scoped !is TestClass.Inner) {
    return "FAIL: expected TestClass.Inner scoped binding but got $scoped"
  }

  return "OK"
}
