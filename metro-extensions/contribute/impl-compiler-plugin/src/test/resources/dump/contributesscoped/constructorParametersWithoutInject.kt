// RUN_PIPELINE_TILL: BACKEND
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

class ScopedDependency

class AnotherScopedDependency

@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass(
  val dependency: ScopedDependency,
  val anotherDependency: AnotherScopedDependency,
) : SuperType, Scoped
