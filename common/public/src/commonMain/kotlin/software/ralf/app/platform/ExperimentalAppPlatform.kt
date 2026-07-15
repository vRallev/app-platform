package software.ralf.app.platform

/**
 * Marks App Platform APIs that are experimental and may change without preserving source or binary
 * compatibility.
 */
@MustBeDocumented
@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "This App Platform API is experimental and may change without preserving source or " +
      "binary compatibility.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.ANNOTATION_CLASS,
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalAppPlatform
