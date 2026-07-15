@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.descriptors.runtime.structure.parameterizedTypeArguments
import org.junit.jupiter.api.Test
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.ralf.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.compile
import software.ralf.app.platform.inject.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.mock.MockMode
import software.ralf.app.platform.inject.mock.RealImpl
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.inner

class ContributesRealImplGeneratorTest {

  @Test
  fun `correct provides method is generated when boundType is inferred`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesRealImpl(AppScope::class)
            class RealImpl : Base
            """
    ) {
      val component = realImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(realImpl)
      assertThat(providesMethod.returnType).isEqualTo(base)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
      assertThat(providesMethod.getAnnotation(RealImpl::class.java)).isNotNull()
    }
  }

  @Test
  fun `correct provides method for inner class is generated`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            class RealImpl {
                @ContributesRealImpl(AppScope::class)
                class Inner : Base
            }
            """
    ) {
      val component = realImpl.inner.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(realImpl.inner)
      assertThat(providesMethod.returnType).isEqualTo(base)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
      assertThat(providesMethod.getAnnotation(RealImpl::class.java)).isNotNull()
    }
  }

  @Test
  fun `repeated annotations produce correct component`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesRealImpl(AppScope::class, boundType = Base::class)
            @ContributesRealImpl(AppScope::class, boundType = Base2::class)
            class RealImpl : Base, Base2
            """
    ) {
      val component = realImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod1 =
        component.declaredNonSyntheticMethods.single { it.name == "provideBaseRealImpl" }
      val providesMethod2 =
        component.declaredNonSyntheticMethods.single { it.name == "provideBase2RealImpl" }

      assertThat(providesMethod1.parameters[0].type).isEqualTo(realImpl)
      assertThat(providesMethod1.returnType).isEqualTo(base)
      assertThat(providesMethod1.getAnnotation(Provides::class.java)).isNotNull()
      assertThat(providesMethod1.getAnnotation(RealImpl::class.java)).isNotNull()

      assertThat(providesMethod2.parameters[0].type).isEqualTo(realImpl)
      assertThat(providesMethod2.returnType).isEqualTo(base2)
      assertThat(providesMethod2.getAnnotation(Provides::class.java)).isNotNull()
      assertThat(providesMethod2.getAnnotation(RealImpl::class.java)).isNotNull()
    }
  }

  @Test
  fun `repeated annotations of the same class type throws error`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesRealImpl(AppScope::class, boundType = Base::class)
            @ContributesRealImpl(AppScope::class, boundType = Base::class)
            class RealImpl : Base, Base2
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains("The same type should not be contributed twice: software.ralf.test.Base.")
    }
  }

  @Test
  fun `repeated annotations of different scopes throws error`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesRealImpl(AppScope::class, boundType = Base::class)
            @ContributesRealImpl(Unit::class, boundType = Base2::class)
            class RealImpl : Base, Base2
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages).contains("All scopes on annotations must be the same.")
    }
  }

  @Test
  fun `when no superType is defined, then an error is thrown`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRealImpl(AppScope::class)
            class RealImpl
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "The bound type could not be determined for RealImpl. " + "There are no super types."
        )
    }
  }

  @Test
  fun `an abstract class as bound type is supported`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            open class Base
            
            @ContributesRealImpl(AppScope::class)
            class RealImpl : Base()
            """
    ) {
      assertThat(realImpl.component).isNotNull()
    }
  }

  @Test
  fun `the bound type can be different than the super type`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base : Base2
            interface Base2
            
            @ContributesRealImpl(AppScope::class, boundType = Base2::class)
            class RealImpl : Base
            """
    ) {
      val component = realImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(realImpl)
      assertThat(providesMethod.returnType).isEqualTo(base2)
      assertThat(providesMethod.name).isEqualTo("provideBase2RealImpl")

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
      assertThat(providesMethod.getAnnotation(RealImpl::class.java)).isNotNull()
    }
  }

  @Test
  fun `the bound type must be declared for multiple super types`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesRealImpl(AppScope::class)
            class RealImpl : Base, Base2
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "The bound type could not be determined for RealImpl. " +
            "There are multiple super types: Base, Base2."
        )
    }
  }

  @Test
  fun `a provides method for the Scoped type is generated`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesRealImpl(AppScope::class)
            class RealImpl : Base, Scoped
            """
    ) {
      val component = realImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      with(component.declaredNonSyntheticMethods.single { it.name == "provideBaseRealImpl" }) {
        assertThat(parameters[0].type).isEqualTo(realImpl)
        assertThat(returnType).isEqualTo(base)

        assertThat(getAnnotation(Provides::class.java)).isNotNull()
        assertThat(getAnnotation(RealImpl::class.java)).isNotNull()
      }

      with(component.declaredNonSyntheticMethods.single { it.name == "provideRealImplScoped" }) {
        assertThat(parameters[0].annotations.single().annotationClass).isEqualTo(MockMode::class)
        assertThat(parameters[1].parameterizedType.parameterizedTypeArguments.single())
          .isEqualTo(realImpl)

        assertThat(getAnnotation(Provides::class.java)).isNotNull()
        assertThat(getAnnotation(IntoSet::class.java)).isNotNull()
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `a provides method for the Scoped type is skipped when the class is annotated with @ContributesBinding`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

            interface Base
            interface Base2
            
            @ContributesRealImpl(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Base2::class)
            class RealImpl : Base, Base2, Scoped
            """
    ) {
      val component = realImpl.component

      with(component.declaredNonSyntheticMethods.single { it.name == "provideBaseRealImpl" }) {
        assertThat(parameters[0].type).isEqualTo(realImpl)
        assertThat(returnType).isEqualTo(base)

        assertThat(getAnnotation(Provides::class.java)).isNotNull()
        assertThat(getAnnotation(RealImpl::class.java)).isNotNull()
      }

      assertThat(
          component.declaredNonSyntheticMethods.firstOrNull { it.name == "provideRealImplScoped" }
        )
        .isNull()
    }
  }

  @Test
  fun `another super type besides Scoped is required`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            
            @ContributesRealImpl(AppScope::class)
            class RealImpl : Scoped
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages).contains("Scoped cannot be used as bound type.")
    }
  }

  private val JvmCompilationResult.base: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Base")

  private val JvmCompilationResult.base2: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Base2")

  private val JvmCompilationResult.realImpl: Class<*>
    get() = classLoader.loadClass("software.ralf.test.RealImpl")

  private val Class<*>.component: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "RealImplComponent"
      )
}
