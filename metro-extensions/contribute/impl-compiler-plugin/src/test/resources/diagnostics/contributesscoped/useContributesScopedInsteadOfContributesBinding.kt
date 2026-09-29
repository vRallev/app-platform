// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.scope.Scoped

interface SuperType

@Inject
@SingleIn(AppScope::class)
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<SuperType>(),
)<!>
class TestClass : SuperType, Scoped
