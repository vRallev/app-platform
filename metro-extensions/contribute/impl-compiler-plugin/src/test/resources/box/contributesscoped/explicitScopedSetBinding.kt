package com.test

import software.ralf.app.platform.scope.Scoped

interface SuperType

interface OtherSuperType

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SuperType>())
@ContributesBinding(AppScope::class, binding = binding<OtherSuperType>())
@ContributesIntoSet(
  AppScope::class,
  binding = binding<@ForScope(AppScope::class) Scoped>(),
)
class TestClass : SuperType, OtherSuperType, Scoped

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val superTypeInstance: SuperType

  val otherSuperTypeInstance: OtherSuperType

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

fun box(): String {
  val graph = createGraph<GraphInterface>()
  val scoped = graph.allScoped.single()
  if (graph.superTypeInstance !== scoped) {
    return "FAIL: expected SuperType to share the scoped singleton"
  }
  if (graph.otherSuperTypeInstance !== scoped) {
    return "FAIL: expected OtherSuperType to share the scoped singleton"
  }

  return "OK"
}
