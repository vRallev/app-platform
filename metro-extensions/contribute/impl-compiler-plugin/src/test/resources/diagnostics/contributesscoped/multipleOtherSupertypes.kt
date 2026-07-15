// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

interface SuperType2

@Inject
@SingleIn(AppScope::class)
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesScoped(AppScope::class)<!>
class TestClass : SuperType, SuperType2, Scoped
