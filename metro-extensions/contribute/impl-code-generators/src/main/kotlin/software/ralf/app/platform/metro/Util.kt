package software.ralf.app.platform.metro

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import dev.zacsweers.metro.Origin

/** The package in which the App Platform extensions generate code. */
internal const val METRO_LOOKUP_PACKAGE = "app.platform.inject.metro"

internal fun TypeSpec.Builder.addMetroOriginAnnotation(
  clazz: KSClassDeclaration
): TypeSpec.Builder =
  addAnnotation(
    AnnotationSpec.builder(Origin::class).addMember("%T::class", clazz.toClassName()).build()
  )
