package software.ralf.app.platform.sample.user

import software.ralf.app.platform.scope.Scope

/**
 * Fake implementation of [User], which is useful in unit tests.
 *
 * This class is part of the `:testing` module and shared with other modules.
 */
class FakeUser(
  override val userId: Long = 1L,
  override val attributes: List<User.Attribute> =
    listOf(fakeAttribute1, fakeAttribute2, fakePicture),
  override val scope: Scope = Scope.buildRootScope(),
) : User {
  companion object {
    /** Fake attribute in tests that is added by default to a [FakeUser] unless overridden. */
    val fakeAttribute1 = User.Attribute("Key1", "Value1")

    /** Fake attribute in tests that is added by default to a [FakeUser] unless overridden. */
    val fakeAttribute2 = User.Attribute("Key2", "Value2")

    /** Fake attribute in tests that is added by default to a [FakeUser] unless overridden. */
    val fakePicture = User.Attribute(User.Attribute.PICTURE_KEY, "picture", metadata = true)
  }
}
