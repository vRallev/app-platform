package software.ralf.app.platform.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.TestComponent
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantSelector
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

public class FakeAndroidApplicationPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
    target.addFakeAndroidComponents()
  }
}

public class FakeAndroidLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
    target.addFakeAndroidComponents()
  }
}

public class FakeAndroidKmpLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) = Unit
}

public class FakeKotlinAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeKotlinJvmPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeMetroPlugin : Plugin<Project> {
  override fun apply(target: Project) = Unit
}

private fun Project.addSinglePlatformConfigurations() {
  listOf(
      "api",
      "implementation",
      "testImplementation",
      "testCompileClasspath",
      "androidTestImplementation",
      "ksp",
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      "${PLUGIN_CLASSPATH_CONFIGURATION_NAME}Debug",
      NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME,
    )
    .forEach { configurationName -> configurations.maybeCreate(configurationName) }

  configurations
    .getByName("testCompileClasspath")
    .extendsFrom(configurations.getByName("testImplementation"))
}

@Suppress("CyclomaticComplexMethod")
private fun Project.addFakeAndroidComponents() {
  val mainCompileClasspath =
    configurations.maybeCreate("debugCompileClasspath").apply {
      extendsFrom(configurations.getByName("implementation"))
    }
  val testCompileClasspath =
    configurations.maybeCreate("debugUnitTestCompileClasspath").apply {
      extendsFrom(configurations.getByName("testImplementation"))
    }
  val testComponent =
    fake(TestComponent::class.java) { _, method, _ ->
      when (method.name) {
        "getName" -> "debugUnitTest"
        "getCompileConfiguration" -> testCompileClasspath
        else -> method.defaultReturnValue()
      }
    }
  val variant =
    fake(Variant::class.java) { _, method, _ ->
      when (method.name) {
        "getName" -> "debug"
        "getCompileConfiguration" -> mainCompileClasspath
        "getNestedComponents" -> listOf(testComponent)
        else -> method.defaultReturnValue()
      }
    }
  val selector =
    fake(VariantSelector::class.java) { proxy, method, _ ->
      if (method.returnType == VariantSelector::class.java) proxy else method.defaultReturnValue()
    }
  val androidComponents =
    fake(AndroidComponentsExtension::class.java) { _, method, arguments ->
      when (method.name) {
        "selector" -> selector
        "onVariants" -> {
          val callback = requireNotNull(arguments).last()
          if (callback is Action<*>) {
            @Suppress("UNCHECKED_CAST") (callback as Action<Variant>).execute(variant)
          } else {
            @Suppress("UNCHECKED_CAST") (callback as (Variant) -> Unit)(variant)
          }
        }
        else -> method.defaultReturnValue()
      }
    }

  extensions.add(AndroidComponentsExtension::class.java, "androidComponents", androidComponents)
}

private fun <T> fake(
  type: Class<T>,
  invocation: (proxy: Any, method: Method, arguments: Array<out Any?>?) -> Any?,
): T =
  type.cast(
    Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, arguments ->
      invocation(proxy, method, arguments)
    }
  )

private fun Method.defaultReturnValue(): Any? =
  when (returnType) {
    Boolean::class.javaPrimitiveType -> false
    Byte::class.javaPrimitiveType -> 0.toByte()
    Short::class.javaPrimitiveType -> 0.toShort()
    Int::class.javaPrimitiveType -> 0
    Long::class.javaPrimitiveType -> 0L
    Float::class.javaPrimitiveType -> 0F
    Double::class.javaPrimitiveType -> 0.0
    Char::class.javaPrimitiveType -> 0.toChar()
    else -> null
  }
