package software.ralf.app.platform.metro.compiler.robot

import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.ClassIds

internal object ContributesRobotIds {
  val CONTRIBUTES_ROBOT_CLASS_ID = ClassIds.CONTRIBUTES_ROBOT
  val CONTRIBUTES_ROBOT_FQ_NAME = FqName("software.ralf.app.platform.inject.robot.ContributesRobot")
  val NESTED_INTERFACE_NAME: Name = Name.identifier("RobotContribution")
  val NESTED_ROBOT_GRAPH_INTERFACE_NAME: Name = Name.identifier("RobotGraphContribution")

  val PREDICATE = LookupPredicate.create { annotated(CONTRIBUTES_ROBOT_FQ_NAME) }

  fun generatedClassNamePrefix(contributingClassId: ClassId): String {
    return contributingClassId.relativeClassName.pathSegments().joinToString(separator = "") {
      it.asString()
    }
  }

  fun generatedRobotPropertyName(contributingClassId: ClassId): Name {
    return Name.identifier(
      generatedClassNamePrefix(contributingClassId).replaceFirstChar { char -> char.lowercase() }
    )
  }
}
