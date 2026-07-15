@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import java.lang.reflect.WildcardType
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
import software.ralf.app.platform.inject.componentInterface
import software.ralf.app.platform.inject.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.mock.MockMode
import software.ralf.app.platform.inject.mock.RealImpl
import software.ralf.app.platform.inject.newComponent
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.inner
import software.ralf.app.platform.scope.Scoped

class ContributesMockImplGeneratorTest {

  @Test
  fun `correct provides method is generated`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base    
            """
    ) {
      val component = mockImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(Boolean::class.java)
      assertThat(providesMethod.parameters[1].parameterizedType.parameterizedTypeArguments.single())
        .isEqualTo(mockImpl)
      assertThat(
          providesMethod.parameters[2]
            .parameterizedType
            .parameterizedTypeArguments
            .filterIsInstance<WildcardType>()
            .single()
            .upperBounds
            .single()
        )
        .isEqualTo(base)
      assertThat(providesMethod.parameters[2].annotations.single().annotationClass)
        .isEqualTo(RealImpl::class)
      assertThat(providesMethod.returnType).isEqualTo(base)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
    }
  }

  @Test
  fun `correct provides method is generated with boundType`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            class MockImpl : Base    
            """
    ) {
      val component = mockImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(Boolean::class.java)
      assertThat(providesMethod.parameters[1].parameterizedType.parameterizedTypeArguments.single())
        .isEqualTo(mockImpl)
      assertThat(
          providesMethod.parameters[2]
            .parameterizedType
            .parameterizedTypeArguments
            .filterIsInstance<WildcardType>()
            .single()
            .upperBounds
            .single()
        )
        .isEqualTo(base)
      assertThat(providesMethod.parameters[2].annotations.single().annotationClass)
        .isEqualTo(RealImpl::class)
      assertThat(providesMethod.returnType).isEqualTo(base)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
    }
  }

  @Test
  fun `correct provides method for inner class is generated`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            class MockImpl {
                @ContributesMockImpl(AppScope::class)
                class Inner : Base
            }
            """
    ) {
      val component = mockImpl.inner.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(Boolean::class.java)
      assertThat(providesMethod.parameters[1].parameterizedType.parameterizedTypeArguments.single())
        .isEqualTo(mockImpl.inner)
      assertThat(
          providesMethod.parameters[2]
            .parameterizedType
            .parameterizedTypeArguments
            .filterIsInstance<WildcardType>()
            .single()
            .upperBounds
            .single()
        )
        .isEqualTo(base)
      assertThat(providesMethod.parameters[2].annotations.single().annotationClass)
        .isEqualTo(RealImpl::class)
      assertThat(providesMethod.returnType).isEqualTo(base)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
    }
  }

  @Test
  fun `an abstract class as bound type is supported`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            open class Base
            
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base()
            """
    ) {
      assertThat(mockImpl.component).isNotNull()
    }
  }

  @Test
  fun `repeated annotations produce correct component`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            @ContributesMockImpl(AppScope::class, boundType = Base2::class)
            class MockImpl : Base, Base2
            """
    ) {
      val component = mockImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      assertThat(component.declaredNonSyntheticMethods.map { it.name }).contains("provideBase")
      assertThat(component.declaredNonSyntheticMethods.map { it.name }).contains("provideBase2")
    }
  }

  @Test
  fun `repeated annotations of the same class type throws error`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            class MockImpl : Base, Base2
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
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            @ContributesMockImpl(Unit::class, boundType = Base2::class)
            class MockImpl : Base, Base2
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
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesMockImpl(AppScope::class)
            class MockImpl
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "The bound type could not be determined for MockImpl. " + "There are no super types."
        )
    }
  }

  @Test
  fun `the bound type can be different than the super type`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base : Base2
            interface Base2
            
            @ContributesMockImpl(AppScope::class, boundType = Base2::class)
            class MockImpl : Base
            """
    ) {
      val component = mockImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      val providesMethod = component.declaredNonSyntheticMethods.single()
      assertThat(providesMethod.parameters[0].type).isEqualTo(Boolean::class.java)
      assertThat(providesMethod.parameters[1].parameterizedType.parameterizedTypeArguments.single())
        .isEqualTo(mockImpl)
      assertThat(
          providesMethod.parameters[2]
            .parameterizedType
            .parameterizedTypeArguments
            .filterIsInstance<WildcardType>()
            .single()
            .upperBounds
            .single()
        )
        .isEqualTo(base2)
      assertThat(providesMethod.parameters[2].annotations.single().annotationClass)
        .isEqualTo(RealImpl::class)
      assertThat(providesMethod.returnType).isEqualTo(base2)

      assertThat(providesMethod.getAnnotation(Provides::class.java)).isNotNull()
    }
  }

  @Test
  fun `the bound type must be declared for multiple super types`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            interface Base2
            
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base, Base2
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "The bound type could not be determined for MockImpl. " +
            "There are multiple super types: Base, Base2."
        )
    }
  }

  @Test
  fun `a provides method for the Scoped type is generated`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface Base
            
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base, Scoped
            """
    ) {
      val component = mockImpl.component

      assertThat(component.getAnnotation(ContributesTo::class.java)?.scope)
        .isEqualTo(AppScope::class)

      with(component.declaredNonSyntheticMethods.single { it.name == "provideBase" }) {
        assertThat(parameters[0].type).isEqualTo(Boolean::class.java)
        assertThat(parameters[1].parameterizedType.parameterizedTypeArguments.single())
          .isEqualTo(mockImpl)
        assertThat(
            parameters[2]
              .parameterizedType
              .parameterizedTypeArguments
              .filterIsInstance<WildcardType>()
              .single()
              .upperBounds
              .single()
          )
          .isEqualTo(base)
        assertThat(parameters[2].annotations.single().annotationClass).isEqualTo(RealImpl::class)
        assertThat(returnType).isEqualTo(base)

        assertThat(getAnnotation(Provides::class.java)).isNotNull()
      }

      with(component.declaredNonSyntheticMethods.single { it.name == "provideMockImplScoped" }) {
        assertThat(parameters[0].annotations.single().annotationClass).isEqualTo(MockMode::class)
        assertThat(parameters[1].parameterizedType.parameterizedTypeArguments.single())
          .isEqualTo(mockImpl)

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
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

            interface Base
            interface Base2
            
            @ContributesMockImpl(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Base2::class)
            class MockImpl : Base, Base2, Scoped
            """
    ) {
      val component = mockImpl.component

      assertThat(component.declaredNonSyntheticMethods.firstOrNull { it.name == "provideBase" })
        .isNotNull()

      assertThat(
          component.declaredNonSyntheticMethods.firstOrNull { it.name == "provideMockImplScoped" }
        )
        .isNull()
    }
  }

  @Test
  fun `another super type besides Scoped is required`() {
    compile(
      """
            package software.ralf.test
            
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.scope.Scoped
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Scoped
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages).contains("Scoped cannot be used as bound type.")
    }
  }

  @Test
  fun `the mock or real impl are provided based on the mock mode flag`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.renderer.RendererComponent
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.inject.mock.MockMode
            import software.ralf.app.platform.robot.RobotComponent
            import me.tatarka.inject.annotations.Component
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.Provides
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesRealImpl(AppScope::class)
            class RealBaseImpl : Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class])
            @SingleIn(AppScope::class)
            abstract class ComponentInterface(
                @get:Provides @get:MockMode val mockMode: Boolean,
            ) : ComponentInterfaceMerged {
                abstract val base: Base
            }
            """
    ) {
      val componentMockModeTrue = componentInterface.newComponent<Any>(true)
      val componentMockModeFalse = componentInterface.newComponent<Any>(false)

      assertThat(
          componentMockModeTrue::class
              .java
              .declaredNonSyntheticMethods
              .single { it.name == "getBase" }
              .invoke(componentMockModeTrue)::class
            .java
        )
        .isEqualTo(mockImpl)

      assertThat(
          componentMockModeFalse::class
              .java
              .declaredNonSyntheticMethods
              .single { it.name == "getBase" }
              .invoke(componentMockModeFalse)::class
            .java
        )
        .isEqualTo(realBaseImpl)
    }
  }

  @Test
  fun `the mock or real impl are provided in the Scoped set based on the mock mode flag`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.renderer.RendererComponent
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.inject.mock.MockMode
            import software.ralf.app.platform.robot.RobotComponent
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Component
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.Provides
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ForScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesRealImpl(AppScope::class)
            class RealBaseImpl : Base, Scoped

            @Inject
            @SingleIn(AppScope::class)
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base, Scoped

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class])
            @SingleIn(AppScope::class)
            abstract class ComponentInterface(
                @get:Provides @get:MockMode val mockMode: Boolean,
            ) : ComponentInterfaceMerged {
                abstract val base: Base
                
                @ForScope(AppScope::class)
                abstract val scoped: Set<Scoped>
            }
            """
    ) {
      val componentMockModeTrue = componentInterface.newComponent<Any>(true)
      val componentMockModeFalse = componentInterface.newComponent<Any>(false)

      @Suppress("UNCHECKED_CAST")
      with(
        componentMockModeTrue::class
          .java
          .declaredNonSyntheticMethods
          .single { it.name == "getScoped" }
          .invoke(componentMockModeTrue) as Set<Scoped>
      ) {
        assertThat(this).hasSize(2)
        assertThat(singleOrNull { mockImpl.isAssignableFrom(it.javaClass) }).isNotNull()
        assertThat(this).contains(Scoped.NO_OP)
      }

      @Suppress("UNCHECKED_CAST")
      with(
        componentMockModeFalse::class
          .java
          .declaredNonSyntheticMethods
          .single { it.name == "getScoped" }
          .invoke(componentMockModeFalse) as Set<Scoped>
      ) {
        assertThat(this).hasSize(2)
        assertThat(singleOrNull { realBaseImpl.isAssignableFrom(it.javaClass) }).isNotNull()
        assertThat(this).contains(Scoped.NO_OP)
      }
    }
  }

  @Test
  fun `a contributed real impl and mock impl can be excluded`() {
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.renderer.RendererComponent
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.inject.mock.MockMode
            import software.ralf.app.platform.robot.RobotComponent
            import me.tatarka.inject.annotations.Component
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.Provides
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesRealImpl(AppScope::class)
            class RealBaseImpl : Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class, RealBaseImpl::class, MockImpl::class])
            @SingleIn(AppScope::class)
            abstract class ComponentInterface(
                @get:Provides @get:MockMode val mockMode: Boolean,
            ) : ComponentInterfaceMerged {
                abstract val base: Base
            }
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains("Cannot find an @Inject constructor or provider for: software.ralf.test.Base")
    }

    // Test again and verify through the Scoped interface
    compile(
      """
            package software.ralf.test
    
            import software.ralf.app.platform.renderer.RendererComponent
            import software.ralf.app.platform.inject.mock.ContributesMockImpl
            import software.ralf.app.platform.inject.mock.ContributesRealImpl
            import software.ralf.app.platform.inject.mock.MockMode
            import software.ralf.app.platform.robot.RobotComponent
            import software.ralf.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Component
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.IntoSet
            import me.tatarka.inject.annotations.Provides
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ForScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesRealImpl(AppScope::class)
            class RealBaseImpl : Base, Scoped

            @Inject
            @SingleIn(AppScope::class)
            @ContributesMockImpl(AppScope::class)
            class MockImpl : Base, Scoped

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class, RealBaseImpl::class, MockImpl::class])
            @SingleIn(AppScope::class)
            abstract class ComponentInterface : ComponentInterfaceMerged {
                
                @ForScope(AppScope::class)
                abstract val scoped: Set<Scoped>
                
                @Provides
                @IntoSet
                @ForScope(AppScope::class)
                fun provideTestScoped(): Scoped = TestScoped
            }

            object TestScoped : Scoped
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

      assertThat(scoped.single().javaClass.canonicalName).isEqualTo("software.ralf.test.TestScoped")
    }
  }

  private val JvmCompilationResult.base: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Base")

  private val JvmCompilationResult.base2: Class<*>
    get() = classLoader.loadClass("software.ralf.test.Base2")

  private val JvmCompilationResult.mockImpl: Class<*>
    get() = classLoader.loadClass("software.ralf.test.MockImpl")

  private val JvmCompilationResult.realBaseImpl: Class<*>
    get() = classLoader.loadClass("software.ralf.test.RealBaseImpl")

  private val Class<*>.component: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "MockImplComponent"
      )
}
