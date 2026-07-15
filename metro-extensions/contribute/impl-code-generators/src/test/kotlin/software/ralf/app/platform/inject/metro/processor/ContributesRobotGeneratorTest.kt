@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.metro.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.ralf.app.platform.inject.metro.compile
import software.ralf.app.platform.inject.metro.declaredNonSyntheticMethods
import software.ralf.app.platform.inject.metro.graphInterface
import software.ralf.app.platform.inject.metro.newMetroGraph
import software.ralf.app.platform.ksp.capitalize
import software.ralf.app.platform.ksp.isAnnotatedWith
import software.ralf.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.ralf.app.platform.renderer.metro.RobotKey
import software.ralf.app.platform.robot.Robot
import software.ralf.test.TestRobotGraph

class ContributesRobotGeneratorTest {

  @Test
  fun `a graph interface is generated without @Inject constructor`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      @ContributesRobot(AppScope::class)
      class TestRobot : Robot
      """,
      graphInterfaceSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(robotGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type).isEqualTo(Function0::class.java)
        assertThat(returnType).isEqualTo(Robot::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(RobotKey::class.java).value.java).isEqualTo(testRobot)
      }

      assertThat(graphInterface.newMetroGraph<TestRobotGraph>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a graph interface is generated with @Inject constructor`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject

      @Inject
      @ContributesRobot(AppScope::class)
      class TestRobot : Robot
      """,
      graphInterfaceSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(robotGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      assertThat(
          robotGraph.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type).isEqualTo(Function0::class.java)
        assertThat(returnType).isEqualTo(Robot::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(RobotKey::class.java).value.java).isEqualTo(testRobot)
      }

      assertThat(graphInterface.newMetroGraph<TestRobotGraph>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a graph interface skips provider for an @Inject robot with constructor parameters`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject

      @Inject
      @ContributesRobot(AppScope::class)
      class TestRobot(
        private val dependency: String,
      ) : Robot {
        fun value(): String = dependency
      }
      """,
      graphInterfaceWithStringSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(
          robotGraph.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      val robot = graphInterface.newMetroGraph<TestRobotGraph>().robots[testRobot.kotlin]!!()
      assertThat(testRobot.getMethod("value").invoke(robot)).isEqualTo("dependency")
    }
  }

  @Test
  fun `a graph interface skips provider for a robot with an @Inject secondary constructor`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject

      @ContributesRobot(AppScope::class)
      class TestRobot private constructor(
        private val dependency: String,
        private val marker: String,
      ) : Robot {
        @Inject constructor(dependency: String) : this(dependency, "injected")

        fun value(): String = dependency + " " + marker
      }
      """,
      graphInterfaceWithStringSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(
          robotGraph.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      val robot = graphInterface.newMetroGraph<TestRobotGraph>().robots[testRobot.kotlin]!!()
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
      import dev.zacsweers.metro.AppScope

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
  fun `a graph interface is generated for constructor parameters without @Inject`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      @ContributesRobot(AppScope::class)
      class TestRobot(
        private val dependency: String,
      ) : Robot {
        fun value(): String = dependency
      }
      """,
      graphInterfaceWithStringSource,
    ) {
      val robotGraph = testRobot.graph

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters.single().type).isEqualTo(String::class.java)
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
      }

      val robot = graphInterface.newMetroGraph<TestRobotGraph>().robots[testRobot.kotlin]!!()
      assertThat(testRobot.getMethod("value").invoke(robot)).isEqualTo("dependency")
    }
  }

  @Test
  fun `a graph interface is generated without direct super type`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      interface BaseRobot1 : Robot
      abstract class BaseRobot2 : BaseRobot1

      @ContributesRobot(AppScope::class)
      class TestRobot : BaseRobot2()
      """
    ) {
      assertThat(testRobot.graph).isNotNull()
    }
  }

  @Test
  fun `the robot class must be a super type`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

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
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

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
  fun `only the app scope is supported for now`() {
    compile(
      """
      package software.ralf.test

      import software.ralf.app.platform.inject.robot.ContributesRobot
      import software.ralf.app.platform.robot.Robot
      import software.amazon.lastmile.kotlin.inject.anvil.AppScope

      @ContributesRobot(String::class)
      class TestRobot : Robot
              """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "Robots can only be contributed to the AppScope for now. " +
            "Scope kotlin.String is unsupported."
        )
    }
  }

  @Language("kotlin")
  private val graphInterfaceSource =
    """
        package software.ralf.test

        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.createGraph
        import dev.zacsweers.metro.DependencyGraph
        import dev.zacsweers.metro.SingleIn

        @DependencyGraph(AppScope::class)
        @SingleIn(AppScope::class)
        interface GraphInterface : TestRobotGraph {
            companion object {
                fun create(): GraphInterface = createGraph<GraphInterface>()
            }
        }
    """

  @Language("kotlin")
  private val graphInterfaceWithStringSource =
    """
        package software.ralf.test

        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.createGraph
        import dev.zacsweers.metro.DependencyGraph
        import dev.zacsweers.metro.Provides
        import dev.zacsweers.metro.SingleIn

        @DependencyGraph(AppScope::class)
        @SingleIn(AppScope::class)
        interface GraphInterface : TestRobotGraph {
            @Provides fun provideString(): String = "dependency"

            companion object {
                fun create(): GraphInterface = createGraph<GraphInterface>()
            }
        }
    """

  private val JvmCompilationResult.testRobot: Class<*>
    get() = classLoader.loadClass("software.ralf.test.TestRobot")

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
