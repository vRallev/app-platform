@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.ralf.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.compile
import software.ralf.app.platform.inject.componentInterface
import software.ralf.app.platform.inject.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.newComponent
import software.ralf.app.platform.inject.origin
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.isAnnotatedWith
import software.ralf.app.platform.robot.RobotComponent

class ContributesRobotGeneratorTest {

  @Test
  fun `a component interface is generated without @Inject constructor`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRobot(AppScope::class)
            class TestRobot : Robot
            """,
      componentInterfaceSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(robotComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)
      assertThat(robotComponent.interfaces.toList()).contains(RobotComponent::class.java)
      assertThat(robotComponent.origin).isEqualTo(testRobot)

      with(robotComponent.declaredNonSyntheticMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(
        robotComponent.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }
      ) {
        assertThat(parameters.single().type.canonicalName)
          .isEqualTo("kotlin.jvm.functions.Function0")
        assertThat(returnType).isEqualTo(Pair::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
      }

      assertThat(componentInterface.newComponent<RobotComponent>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a component interface is generated with @Inject constructor`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @Inject
            @ContributesRobot(AppScope::class)
            class TestRobot : Robot
            """,
      componentInterfaceSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(robotComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)
      assertThat(robotComponent.origin).isEqualTo(testRobot)

      assertThat(
          robotComponent.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      with(
        robotComponent.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }
      ) {
        assertThat(parameters.single().type.canonicalName)
          .isEqualTo("kotlin.jvm.functions.Function0")
        assertThat(returnType).isEqualTo(Pair::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
      }

      assertThat(componentInterface.newComponent<RobotComponent>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a component interface skips provider for an @Inject robot with constructor parameters`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @Inject
            @ContributesRobot(AppScope::class)
            class TestRobot(
              private val dependency: String,
            ) : Robot {
              fun value(): String = dependency
            }
            """,
      componentInterfaceWithStringSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(
          robotComponent.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      val robot = componentInterface.newComponent<RobotComponent>().robots[testRobot.kotlin]!!()
      assertThat(testRobot.getMethod("value").invoke(robot)).isEqualTo("dependency")
    }
  }

  @Test
  fun `a component interface skips provider for a robot with an @Inject secondary constructor`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRobot(AppScope::class)
            class TestRobot private constructor(
              private val dependency: String,
              private val marker: String,
            ) : Robot {
              @Inject constructor(dependency: String) : this(dependency, "injected")

              fun value(): String = dependency + " " + marker
            }
            """,
      componentInterfaceWithStringSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(
          robotComponent.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      val robot = componentInterface.newComponent<RobotComponent>().robots[testRobot.kotlin]!!()
      assertThat(testRobot.getMethod("value").invoke(robot)).isEqualTo("dependency injected")
    }
  }

  @Test
  fun `a robot with multiple constructors must use @Inject`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRobot(AppScope::class)
            class TestRobot(
              private val dependency: String,
            ) : Robot {
              constructor(dependency: String, marker: String) : this(dependency)
            }
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "TestRobot has multiple constructors. Annotate the constructor to use with @Inject, " +
            "or remove the extra constructors so @ContributesRobot can generate a provider."
        )
    }
  }

  @Test
  fun `a component interface is generated for constructor parameters without @Inject`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRobot(AppScope::class)
            class TestRobot(
              private val dependency: String,
            ) : Robot {
              fun value(): String = dependency
            }
            """,
      componentInterfaceWithStringSource,
    ) {
      val robotComponent = testRobot.component

      with(robotComponent.declaredNonSyntheticMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters.single().type).isEqualTo(String::class.java)
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
      }

      val robot = componentInterface.newComponent<RobotComponent>().robots[testRobot.kotlin]!!()
      assertThat(testRobot.getMethod("value").invoke(robot)).isEqualTo("dependency")
    }
  }

  @Test
  fun `a component interface is generated without direct super type`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface BaseRobot1 : Robot
            abstract class BaseRobot2 : BaseRobot1

            @ContributesRobot(AppScope::class)
            class TestRobot : BaseRobot2()
            """
    ) {
      assertThat(testRobot.component).isNotNull()
    }
  }

  @Test
  fun `the robot class must be a super type`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface BaseRobot1
            abstract class BaseRobot2 : BaseRobot1

            @ContributesRobot(AppScope::class)
            class TestRobot : BaseRobot2()
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "In order to use @ContributesRobot, TestRobot must implement " +
            "software.ralf.app.platform.robot.Robot."
        )
    }
  }

  @Test
  fun `a Robot must not be a singleton`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            @Inject
            @SingleIn(AppScope::class)
            @ContributesRobot(AppScope::class)
            class TestRobot : Robot
            """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "It's not allowed allowed for a robot to be a singleton, because " +
            "the lifetime of the robot is scoped to the robot() factory function. " +
            "Remove the @SingleIn annotation."
        )
    }
  }

  @Test
  fun `a custom scope is supported`() {
    compile(
      """
            package software.ralf.test

            import software.ralf.app.platform.inject.robot.ContributesRobot
            import software.ralf.app.platform.robot.Robot
            @ContributesRobot(String::class)
            class TestRobot : Robot
            """,
      customScopeComponentInterfaceSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(robotComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(String::class)
      assertThat(robotComponent.interfaces.toList()).contains(RobotComponent::class.java)
      assertThat(componentInterface.newComponent<RobotComponent>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Language("kotlin")
  private val componentInterfaceSource =
    """
        package software.ralf.test

        import software.ralf.app.platform.renderer.RendererComponent
        import me.tatarka.inject.annotations.Component
        import software.amazon.lastmile.kotlin.inject.anvil.AppScope
        import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
        import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

        @Component
        @MergeComponent(AppScope::class, exclude = [RendererComponent::class])
        @SingleIn(AppScope::class)
        interface ComponentInterface : ComponentInterfaceMerged
    """

  @Language("kotlin")
  private val customScopeComponentInterfaceSource =
    """
        package software.ralf.test

        import me.tatarka.inject.annotations.Component
        import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent

        @Component
        @MergeComponent(String::class)
        interface ComponentInterface : ComponentInterfaceMerged
    """

  @Language("kotlin")
  private val componentInterfaceWithStringSource =
    """
        package software.ralf.test

        import software.ralf.app.platform.renderer.RendererComponent
        import me.tatarka.inject.annotations.Component
        import me.tatarka.inject.annotations.Provides
        import software.amazon.lastmile.kotlin.inject.anvil.AppScope
        import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
        import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

        @Component
        @MergeComponent(AppScope::class, exclude = [RendererComponent::class])
        @SingleIn(AppScope::class)
        interface ComponentInterface : ComponentInterfaceMerged {
          @Provides fun provideString(): String = "dependency"
        }
    """

  private val JvmCompilationResult.testRobot: Class<*>
    get() = classLoader.loadClass("software.ralf.test.TestRobot")

  private val Class<*>.component: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "Component"
      )
}
