package software.ralf.app.platform.metro.compiler.scoped

import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.ClassIds

internal object ContributesScopedIds {
  val CONTRIBUTES_SCOPED_CLASS_ID = ClassIds.CONTRIBUTES_SCOPED
  val NESTED_INTERFACE_NAME: Name = Name.identifier("ScopedContribution")
  val PREDICATE = LookupPredicate.create { annotated(CONTRIBUTES_SCOPED_CLASS_ID.asSingleFqName()) }
  val SINGLE_IN_PREDICATE = LookupPredicate.create {
    annotated(ClassIds.SINGLE_IN.asSingleFqName())
  }

  fun generatedOwnerName(contributingClassId: ClassId): String {
    return contributingClassId.relativeClassName.pathSegments().joinToString(separator = "") {
      it.asString()
    }
  }

  fun generatedTypeName(boundClassId: ClassId): String {
    return boundClassId.relativeClassName.pathSegments().joinToString(separator = "") {
      it.asString()
    }
  }
}
