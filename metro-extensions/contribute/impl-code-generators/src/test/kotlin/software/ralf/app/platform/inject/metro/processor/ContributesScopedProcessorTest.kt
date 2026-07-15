@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.metro.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.ralf.app.platform.inject.metro.compile
import software.ralf.app.platform.inject.metro.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.metro.graphInterface
import software.ralf.app.platform.inject.metro.newMetroGraph
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.inner
import software.ralf.app.platform.ksp.isAnnotatedWith
import software.ralf.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.ralf.app.platform.scope.Scoped

class ContributesScopedProcessorTest {

  @Test
  fun `a graph interface is generated`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.createGraph
      import dev.zacsweers.metro.DependencyGraph
      import dev.zacsweers.metro.ForScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : SuperType, Scoped

      @DependencyGraph(AppScope::class)
      @SingleIn(AppScope::class)
      interface GraphInterface {
        val superTypeInstance: SuperType
        
        @ForScope(AppScope::class)
        val allScoped: Set<Scoped>
      
        companion object {
          fun create(): GraphInterface = createGraph<GraphInterface>()
        }
      }
      """
    ) {
      val scopedGraph = testClass.graph

      assertThat(scopedGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      // The annotations for these functions are defined in other kotlinc generated classes.
      // Instead of relying on reflection, we verify them by running the Metro compiler and
      // instantiating the Metro graph below.
      with(scopedGraph.declaredNonSyntheticMethods.single { it.name == "getBindSuperType" }) {
        assertThat(parameters.single().type).isEqualTo(testClass)
        assertThat(returnType).isEqualTo(superType)
      }

      with(scopedGraph.declaredNonSyntheticMethods.single { it.name == "getBindTestClassScoped" }) {
        assertThat(parameters.single().type).isEqualTo(testClass)
        assertThat(returnType).isEqualTo(Scoped::class.java)
      }

      val graph = graphInterface.newMetroGraph<Any>()

      @Suppress("UNCHECKED_CAST")
      val scopedInstances =
        graphInterface.declaredNonSyntheticMethods
          .single { it.name == "getAllScoped" }
          .invoke(graph) as Set<Scoped>
      assertThat(scopedInstances.single()::class.java).isEqualTo(testClass)

      @Suppress("UNCHECKED_CAST")
      assertThat(
          graphInterface.declaredNonSyntheticMethods
              .single { it.name == "getSuperTypeInstance" }
              .invoke(graph)::class
            .java
        )
        .isEqualTo(testClass)
    }
  }

  @Test
  fun `a graph interface is generated for an inner class`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      interface TestClass {
        @Inject
        @SingleIn(AppScope::class)
        @ContributesScoped(AppScope::class)
        class Inner : SuperType, Scoped
      }
      """
    ) {
      val scopedGraph = testClass.inner.graph

      assertThat(scopedGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      with(scopedGraph.declaredNonSyntheticMethods.single { it.name == "getBindSuperType" }) {
        assertThat(parameters.single().type).isEqualTo(testClass.inner)
        assertThat(returnType).isEqualTo(superType)
      }

      with(
        scopedGraph.declaredNonSyntheticMethods.single { it.name == "getBindTestClassInnerScoped" }
      ) {
        assertThat(parameters.single().type).isEqualTo(testClass.inner)
        assertThat(returnType).isEqualTo(Scoped::class.java)
      }
    }
  }

  @Test
  fun `a graph interface is generated for constructor parameters without @Inject`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.createGraph
      import dev.zacsweers.metro.DependencyGraph
      import dev.zacsweers.metro.ForScope
      import dev.zacsweers.metro.Provides
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass(
        private val dependency: String,
      ) : SuperType, Scoped {
        fun value(): String = dependency
      }

      @DependencyGraph(AppScope::class)
      @SingleIn(AppScope::class)
      interface GraphInterface {
        val superTypeInstance: SuperType

        @ForScope(AppScope::class)
        val allScoped: Set<Scoped>

        @Provides
        fun provideString(): String = "dependency"

        companion object {
          fun create(): GraphInterface = createGraph<GraphInterface>()
        }
      }
      """
    ) {
      val scopedGraph = testClass.graph

      with(scopedGraph.declaredNonSyntheticMethods.single { it.name == "provideTestClass" }) {
        assertThat(parameters.single().type).isEqualTo(String::class.java)
        assertThat(returnType).isEqualTo(testClass)
        assertThat(this).isAnnotatedWith(Provides::class)
      }

      val graph = graphInterface.newMetroGraph<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped =
        graphInterface.declaredNonSyntheticMethods
          .single { it.name == "getAllScoped" }
          .invoke(graph) as Set<Scoped>

      val scopedInstance = scoped.single()
      assertThat(scopedInstance::class.java).isEqualTo(testClass)
      assertThat(testClass.getMethod("value").invoke(scopedInstance)).isEqualTo("dependency")
      assertThat(
          graphInterface.declaredNonSyntheticMethods
            .single { it.name == "getSuperTypeInstance" }
            .invoke(graph)
        )
        .isEqualTo(scopedInstance)
    }
  }

  @Test
  fun `a graph interface skips provider for an @Inject secondary constructor`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.createGraph
      import dev.zacsweers.metro.DependencyGraph
      import dev.zacsweers.metro.ForScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.Provides
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass private constructor(
        private val dependency: String,
        private val marker: String,
      ) : SuperType, Scoped {
        @Inject constructor(dependency: String) : this(dependency, "injected")

        fun value(): String = "${'$'}dependency ${'$'}marker"
      }

      @DependencyGraph(AppScope::class)
      @SingleIn(AppScope::class)
      interface GraphInterface {
        val superTypeInstance: SuperType

        @ForScope(AppScope::class)
        val allScoped: Set<Scoped>

        @Provides
        fun provideString(): String = "dependency"

        companion object {
          fun create(): GraphInterface = createGraph<GraphInterface>()
        }
      }
      """
    ) {
      val scopedGraph = testClass.graph

      assertThat(
          scopedGraph.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestClass" }
        )
        .isNull()

      val graph = graphInterface.newMetroGraph<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped =
        graphInterface.declaredNonSyntheticMethods
          .single { it.name == "getAllScoped" }
          .invoke(graph) as Set<Scoped>

      val scopedInstance = scoped.single()
      assertThat(testClass.getMethod("value").invoke(scopedInstance))
        .isEqualTo("dependency injected")
    }
  }

  @Test
  fun `a graph interface is generated when only Scoped is implemented`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : Scoped
      """
    ) {
      val scopedGraph = testClass.graph

      assertThat(scopedGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      assertThat(scopedGraph.declaredNonSyntheticMethods).hasSize(1)

      with(scopedGraph.declaredNonSyntheticMethods.single { it.name == "getBindTestClassScoped" }) {
        assertThat(parameters.single().type).isEqualTo(testClass)
        assertThat(returnType).isEqualTo(Scoped::class.java)
      }
    }
  }

  @Test
  fun `it's an error when there is no super type`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "In order to use @ContributesScoped, TestClass must implement software.ralf.app.platform.scope.Scoped."
        )
    }
  }

  @Test
  fun `it's an error when Scoped is not implemented`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : SuperType
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "In order to use @ContributesScoped, TestClass must implement software.ralf.app.platform.scope.Scoped."
        )
    }
  }

  @Test
  fun `it's an error when there are multiple super types`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType
      interface SuperType2

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : SuperType, SuperType2, Scoped
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "In order to use @ContributesScoped, TestClass is allowed to have only one other super type besides Scoped."
        )
    }
  }

  @Test
  fun `Scoped can be implemented through another type`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType2 : Scoped
      interface SuperType3 : SuperType2

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : SuperType2
      """
    ) {
      assertThat(testClass.graph).isNotNull()
    }
  }

  @Test
  fun `a scoped class with multiple constructors must use @Inject`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope

      interface SuperType

      @ContributesScoped(AppScope::class)
      class TestClass(
        private val dependency: String,
      ) : SuperType, Scoped {
        constructor(dependency: String, marker: String) : this(dependency)
      }
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "TestClass has multiple constructors. Annotate the constructor to use with @Inject, " +
            "or remove the extra constructors so @ContributesScoped can generate a provider."
        )
    }
  }

  @Test
  fun `using @ContributesBinding when implementing Scoped is an error`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.ContributesBinding
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      @Inject
      @SingleIn(AppScope::class)
      @ContributesBinding(AppScope::class)
      class TestClass : SuperType, Scoped
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "TestClass implements Scoped, but uses @ContributesBinding instead of " +
            "@ContributesScoped. When implementing Scoped the annotation @ContributesScoped " +
            "must be used instead of @ContributesBinding to bind both super types correctly. " +
            "It's not necessary to use @ContributesBinding."
        )
    }
  }

  @Test
  fun `classes using @ContributesScoped can be excluded`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.metro.ContributesScoped
      import software.ralf.app.platform.scope.Scoped
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.createGraph
      import dev.zacsweers.metro.DependencyGraph
      import dev.zacsweers.metro.ForScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      interface SuperType

      @Inject
      @SingleIn(AppScope::class)
      @ContributesScoped(AppScope::class)
      class TestClass : SuperType, Scoped

      @DependencyGraph(AppScope::class, excludes = [TestClass::class])
      @SingleIn(AppScope::class)
      interface GraphInterface {
        val superTypeInstance: SuperType
        
        @ForScope(AppScope::class)
        val allScoped: Set<Scoped>
      
        companion object {
          fun create(): GraphInterface = createGraph<GraphInterface>()
        }
      }
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages).contains("[Metro/MissingBinding] No binding found for SuperType")
    }
  }

  private val JvmCompilationResult.testClass: Class<*>
    get() = classLoader.loadClass("software.ralf.test.TestClass")

  private val JvmCompilationResult.superType: Class<*>
    get() = classLoader.loadClass("software.ralf.test.SuperType")

  private val Class<*>.graph: Class<*>
    get() =
      classLoader.loadClass(
        "$METRO_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "Graph"
      )
}
