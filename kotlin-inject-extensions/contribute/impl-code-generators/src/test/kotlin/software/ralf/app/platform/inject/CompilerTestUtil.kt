package software.ralf.app.platform.inject

import java.lang.reflect.Method

// Following changes to Kotlin starting in 2.2.0,
// https://kotlinlang.org/docs/whatsnew22.html#changes-to-default-method-generation-for-interface-functions
// default methods are generated where they previously weren't. For testing we only validate the non
// synthetic methods.
internal val Class<*>.declaredNonSyntheticMethods: List<Method>
  get() = declaredMethods.filterNot { it.isSynthetic }
