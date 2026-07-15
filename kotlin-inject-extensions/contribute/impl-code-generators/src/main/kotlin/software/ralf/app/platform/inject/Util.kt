package software.ralf.app.platform.inject

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import software.amazon.lastmile.kotlin.inject.anvil.internal.Origin

/**
 * The package in which code is generated that should be picked up during the merging phase. This
 * package is used by the open source project.
 */
internal const val OPEN_SOURCE_LOOKUP_PACKAGE = "amazon.lastmile.inject"

/** The package in which the App Platform extensions generate code. */
internal const val APP_PLATFORM_LOOKUP_PACKAGE = "app.platform.inject"

internal fun <T : Annotatable.Builder<T>> Annotatable.Builder<T>.addOriginAnnotation(
  clazz: KSClassDeclaration
): T =
  addAnnotation(
    AnnotationSpec.builder(Origin::class)
      .addMember("value = %T::class", clazz.toClassName())
      .build()
  )
