package software.ralf.app.platform.metro.compiler.renderer

import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.ClassIds

internal object ContributesRendererIds {
  val CONTRIBUTES_RENDERER_CLASS_ID = ClassIds.CONTRIBUTES_RENDERER
  val CONTRIBUTES_RENDERER_FQ_NAME = ClassIds.CONTRIBUTES_RENDERER.asSingleFqName()
  val NESTED_INTERFACE_NAME: Name = Name.identifier("RendererContribution")

  val PREDICATE = LookupPredicate.create { annotated(CONTRIBUTES_RENDERER_FQ_NAME) }

  fun generatedSafeClassNamePrefix(contributingClassId: ClassId): String {
    return (contributingClassId.packageFqName.pathSegments() +
        contributingClassId.relativeClassName.pathSegments())
      .joinToString(separator = "") { it.asString().replaceFirstChar(Char::uppercase) }
  }

  fun generatedModelClassNameSuffix(modelClassId: ClassId): String {
    return modelClassId.relativeClassName.pathSegments().joinToString(separator = "") {
      it.asString()
    }
  }
}
