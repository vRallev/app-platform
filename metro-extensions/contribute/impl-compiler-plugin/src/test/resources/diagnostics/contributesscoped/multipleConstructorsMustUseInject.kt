// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesScoped(AppScope::class)<!>
class TestClass(
  val dependency: String,
) : SuperType, Scoped {
  constructor(dependency: String, marker: String) : this(dependency)
}
