// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.scope.Scoped

interface SuperType

@Inject
@SingleIn(AppScope::class)
<!AGGREGATION_ERROR, CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(AppScope::class)<!>
class TestClass : SuperType, Scoped
