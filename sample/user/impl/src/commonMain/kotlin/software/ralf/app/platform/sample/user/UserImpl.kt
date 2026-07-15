package software.ralf.app.platform.sample.user

import software.ralf.app.platform.scope.Scope

/** Production implementation of [User]. This is a data class for equals() and hashcode(). */
internal data class UserImpl(
  override val userId: Long,
  override val attributes: List<User.Attribute>,
) : User {
  @Suppress("DataClassShouldBeImmutable")
  override lateinit var scope: Scope
    internal set
}
