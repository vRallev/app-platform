@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.JvmCompilationResult
import kotlin.test.assertFailsWith
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.ralf.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.compile
import software.ralf.app.platform.inject.componentInterface
import software.ralf.app.platform.inject.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.generatedComponent
import software.ralf.app.platform.inject.newComponent
import software.ralf.app.platform.inject.origin
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.inner
import software.ralf.app.platform.ksp.isAnnotatedWith
import software.ralf.app.platform.scope.Scoped

class ContributesBindingScopedProcessorTest {

  @Test
  fun `a binding method for Scoped is generated`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base, Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent

      assertThat(generatedComponent.origin).isEqualTo(impl)
      assertThat(generatedComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      with(
        generatedComponent.declaredNonSyntheticMethods.single { it.name == "provideImplScoped" }
      ) {
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated for inner classes`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            interface Impl {
                @Inject
                @ContributesBinding(Unit::class)
                class Inner : Base, Scoped
            } 
            """
    ) {
      val generatedComponent = impl.inner.scopedComponent

      assertThat(generatedComponent.origin).isEqualTo(impl.inner)
      assertThat(generatedComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(Unit::class)

      with(
        generatedComponent.declaredNonSyntheticMethods.single {
          it.name == "provideImplInnerScoped"
        }
      ) {
        assertThat(parameters.single().type).isEqualTo(impl.inner)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(Unit::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated for repeated annotations`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base
            interface Base2

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Base2::class)
            class Impl : Base, Base2, Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent

      with(
        generatedComponent.declaredNonSyntheticMethods.single { it.name == "provideImplScoped" }
      ) {
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated without any other binding`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent
      with(generatedComponent.declaredNonSyntheticMethods.single()) {
        assertThat(name).isEqualTo("provideImplScoped")
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }

      // Because Scoped is the only super type.
      assertFailsWith<ClassNotFoundException> { impl.generatedComponent }
    }
  }

  @Test
  fun `a binding method for Scoped is generated only explicitly when Scoped is part of the supertype hierarchy`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base : Scoped

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Scoped::class)
            class Impl2 : Base
            """
    ) {
      with(impl.generatedComponent.declaredNonSyntheticMethods.single()) {
        assertThat(name).isEqualTo("provideImplBase")
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(base)
        assertThat(this).isAnnotatedWith(Provides::class)
      }
      // Because Scoped is not a direct super type.
      assertFailsWith<ClassNotFoundException> { impl.scopedComponent }

      with(impl2.generatedComponent.declaredNonSyntheticMethods.single()) {
        assertThat(parameters.single().type).isEqualTo(impl2)
        assertThat(returnType).isEqualTo(base)
        assertThat(this).isAnnotatedWith(Provides::class)
      }
      with(
        impl2.scopedComponent.declaredNonSyntheticMethods.single { it.name == "provideImpl2Scoped" }
      ) {
        assertThat(parameters.single().type).isEqualTo(impl2)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `scoped instances are added to the component`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.renderer.RendererComponent
            import software.ralf.app.platform.robot.RobotComponent
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.Component
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.ForScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base, Scoped

            @Inject
            @SingleIn(Unit::class)
            @ContributesBinding(Unit::class)
            class Impl2 : Base, Scoped

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class])
            @SingleIn(AppScope::class)
            interface ComponentInterface : ComponentInterfaceMerged {
                @ForScope(AppScope::class)
                val scoped: Set<Scoped>
            }

            @Component
            @MergeComponent(Unit::class)
            @SingleIn(Unit::class)
            interface ComponentInterface2 : ComponentInterface2Merged {
                @ForScope(Unit::class)
                val scoped: Set<Scoped>
            }
            """
    ) {
      val component = componentInterface.newComponent<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped =
        component::class
          .java
          .declaredNonSyntheticMethods
          .single { it.name == "getScoped" }
          .invoke(component) as Set<Scoped>

      assertThat(scoped).hasSize(1)
      assertThat(scoped.single()::class.java).isEqualTo(impl)

      val component2 = componentInterface2.newComponent<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped2 =
        component2::class
          .java
          .declaredNonSyntheticMethods
          .single { it.name == "getScoped" }
          .invoke(component2) as Set<Scoped>

      assertThat(scoped2).hasSize(1)
      assertThat(scoped2.single()::class.java).isEqualTo(impl2)
    }
  }

  private val Class<*>.scopedComponent: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter("$packageName.").split(".").joinToString(separator = "") {
            it.capitalize()
          } +
          "ScopedComponent"
      )

  private val JvmCompilationResult.componentInterface2: Class<*>
    get() = classLoader.loadClass("software.ralf.test.ComponentInterface2")

  private val JvmCompilationResult.base: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Base")

  private val JvmCompilationResult.impl: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Impl")

  private val JvmCompilationResult.impl2: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Impl2")

  private val scoped: Class<*>
    get() = Scoped::class.java
}
