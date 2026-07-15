package software.ralf.app.platform.sample.user

/**
 * Marker class for the user scope, which can be used with `@SingleIn(UserScope::class)` or
 * `@ContributesBinding(UserScope::class)`.
 *
 * This is an abstract class with a private constructor so that no instance is allocated at runtime.
 */
abstract class UserScope private constructor()
