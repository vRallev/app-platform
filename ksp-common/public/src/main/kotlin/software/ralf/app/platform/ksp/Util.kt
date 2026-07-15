package software.ralf.app.platform.ksp

import com.google.devtools.ksp.isDefault
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument
import java.util.Locale

public fun String.decapitalize(): String = replaceFirstChar { it.lowercase(Locale.US) }

public fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}

public inline fun <reified T> KSAnnotation.argumentOfTypeAt(
  context: ContextAware,
  name: String,
): T? {
  return argumentOfTypeWithMapperAt<T, T>(context, name) { _, value -> value }
}

public inline fun <reified T, R> KSAnnotation.argumentOfTypeWithMapperAt(
  context: ContextAware,
  name: String,
  mapper: (arg: KSValueArgument, value: T) -> R,
): R? {
  return argumentAt(name)?.let { arg ->
    val value = arg.value
    context.check(value is T, arg) {
      "Expected argument '$name' of type '${T::class.qualifiedName} but was '${arg.javaClass.name}'."
    }
    (value as T)?.let { mapper(arg, it) }
  }
}

public fun KSAnnotation.argumentAt(name: String): KSValueArgument? {
  return arguments.find { it.name?.asString() == name }?.takeUnless { it.isDefault() }
}
