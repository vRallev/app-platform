package software.ralf.app.platform.metro.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object ClassIds {
  val APP_SCOPE = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("AppScope"))

  val BASE_MODEL =
    ClassId(FqName("software.ralf.app.platform.presenter"), Name.identifier("BaseModel"))

  val BINDS = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Binds"))

  val BINDING_CONTAINER =
    ClassId(FqName("dev.zacsweers.metro"), Name.identifier("BindingContainer"))

  val CONTRIBUTES_RENDERER =
    ClassId(FqName("software.ralf.app.platform.inject"), Name.identifier("ContributesRenderer"))

  val CONTRIBUTES_ROBOT =
    ClassId(
      FqName("software.ralf.app.platform.inject.robot"),
      Name.identifier("ContributesRobot"),
    )

  val CONTRIBUTES_SCOPED =
    ClassId(
      FqName("software.ralf.app.platform.inject.metro"),
      Name.identifier("ContributesScoped"),
    )

  val CONTRIBUTES_BINDING =
    ClassId(FqName("dev.zacsweers.metro"), Name.identifier("ContributesBinding"))

  val CONTRIBUTES_TO = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("ContributesTo"))

  val DEPENDENCY_GRAPH = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("DependencyGraph"))

  val FOR_SCOPE = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("ForScope"))

  val INJECT = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Inject"))

  val INTO_MAP = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("IntoMap"))

  val INTO_SET = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("IntoSet"))

  val ORIGIN = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Origin"))

  val PROVIDER = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Provider"))

  val PROVIDES = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Provides"))

  val RENDERER = ClassId(FqName("software.ralf.app.platform.renderer"), Name.identifier("Renderer"))

  val RENDERER_KEY =
    ClassId(FqName("software.ralf.app.platform.renderer.metro"), Name.identifier("RendererKey"))

  val RENDERER_SCOPE =
    ClassId(FqName("software.ralf.app.platform.renderer"), Name.identifier("RendererScope"))

  val ROBOT = ClassId(FqName("software.ralf.app.platform.robot"), Name.identifier("Robot"))

  val ROBOT_GRAPH =
    ClassId(FqName("software.ralf.app.platform.robot"), Name.identifier("RobotGraph"))

  val ROBOT_KEY =
    ClassId(FqName("software.ralf.app.platform.renderer.metro"), Name.identifier("RobotKey"))

  val ROBOT_FQ_NAMES: Set<ClassId> = setOf(ROBOT)

  val SINGLE_IN = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("SingleIn"))

  val SCOPED = ClassId(FqName("software.ralf.app.platform.scope"), Name.identifier("Scoped"))

  val SCOPE = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Scope"))

  val UNIT = ClassId(FqName("kotlin"), Name.identifier("Unit"))
}
