// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped

interface SuperType

@Inject
@SingleIn(AppScope::class)
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesScoped(AppScope::class)<!>
class TestClass : SuperType
