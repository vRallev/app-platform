package software.ralf.app.platform.scope

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ScopeTest {

  @Test
  fun `child scopes can be created`() {
    val root = Scope.buildRootScope("root")
    assertThat(root.name).isEqualTo("root")
    assertThat(root.parent).isNull()

    val child = root.buildChild("child")
    assertThat(child.name).isEqualTo("child")
    assertThat(child.parent).isEqualTo(root)
  }

  @Test
  fun `a scope can have multiple children`() {
    val root = Scope.buildRootScope()
    root.buildChild("child1")
    root.buildChild("child2")
    root.buildChild("child3")

    assertThat(root.children()).hasSize(3)
  }

  @Test
  fun `after a scope is destroyed methods throw`() {
    val root = Scope.buildRootScope()
    assertThat(root.isDestroyed()).isFalse()

    root.destroy()
    assertThat(root.isDestroyed()).isTrue()

    assertFailsWith<IllegalStateException> { root.buildChild("child") }
    assertFailsWith<IllegalStateException> { root.register(Scoped.NO_OP) }
    assertFailsWith<IllegalStateException> { root.getService<Any>("") }

    // Function is idempotent.
    root.destroy()
  }

  @Test
  fun `destroying the parent scope destroys all children`() {
    val root = Scope.buildRootScope()
    val children =
      setOf(root.buildChild("child1"), root.buildChild("child2"), root.buildChild("child3"))

    children.forEach { assertThat(it.isDestroyed()).isFalse() }

    root.destroy()
    children.forEach { assertThat(it.isDestroyed()).isTrue() }
  }

  @Test
  fun `destroying a child removes it from the parent`() {
    val root = Scope.buildRootScope()
    val child = root.buildChild("child")

    assertThat(root.children().single()).isEqualTo(child)

    child.destroy()
    assertThat(root.children()).isEmpty()
  }

  @Test
  fun `a Scoped onEnterScope function is called immediately`() {
    val root = Scope.buildRootScope()
    val scoped = FakeScoped()
    root.register(scoped)

    assertThat(scoped.onEnterCalled).isEqualTo(1)
  }

  @Test
  fun `registering a Scoped multiple times is idempotent`() {
    val root = Scope.buildRootScope()
    val scoped = FakeScoped()

    repeat(10) { root.register(scoped) }
    assertThat(scoped.onEnterCalled).isEqualTo(1)

    repeat(10) { root.destroy() }
    assertThat(scoped.onExitCalled).isEqualTo(1)
  }

  @Test
  fun `destroying a parent scope calls onExitScope for children`() {
    val root = Scope.buildRootScope()
    val child = root.buildChild("child")

    val scoped = FakeScoped()
    child.register(scoped)

    root.destroy()
    assertThat(scoped.onExitCalled).isEqualTo(1)
  }

  @Test
  fun `toString contains meaningful information`() {
    val root = Scope.buildRootScope()
    val child = root.buildChild("child")
    val grandChild = child.buildChild("grand-child")

    assertThat(grandChild.toString()).contains("grand-child")
  }

  @Test
  fun `a service can be registered`() {
    val root = Scope.buildRootScope { addService("key", "value") }

    assertThat(root.getService<String>("key")).isEqualTo("value")
    assertThat(root.getService<Any>("key2")).isNull()
  }

  @Test
  fun `parents returns the chain of parent scopes`() {
    val root = Scope.buildRootScope()
    val child1 = root.buildChild("child1")
    val child2 = root.buildChild("child2")
    val grandchild = child1.buildChild("grandchild")

    assertThat(grandchild.parents(includeSelf = true).toList())
      .containsExactly(grandchild, child1, root)
    assertThat(grandchild.parents(includeSelf = false).toList()).containsExactly(child1, root)

    assertThat(child2.parents(includeSelf = true).toList()).containsExactly(child2, root)
    assertThat(child2.parents(includeSelf = false).toList()).containsExactly(root)

    assertThat(root.parents(includeSelf = true).toList()).containsExactlyInAnyOrder(root)
    assertThat(root.parents(includeSelf = false).toList()).isEmpty()
  }

  @Test
  fun `a Scoped can be registered in the builder`() {
    val scoped = FakeScoped()
    Scope.buildRootScope {
      register(scoped)
      assertThat(scoped.onEnterCalled).isEqualTo(0)
    }
    assertThat(scoped.onEnterCalled).isEqualTo(1)
  }

  @Test
  fun `onExit is invoked when the scope is destroyed`() {
    val root = Scope.buildRootScope()
    val child = root.buildChild("child")

    var onExitCalled = 0

    child.onExit { onExitCalled++ }

    root.destroy()
    assertThat(onExitCalled).isEqualTo(1)
  }

  @Test
  fun `destroy can be called on a scope that is currently being destroyed`() {
    val root = Scope.buildRootScope()
    repeat(3) {
      val child = root.buildChild("child-$it")
      child.onExit {
        // This makes the child destroy the parent while it's being destroyed and that
        // triggered a bug previously.
        root.destroy()
      }
    }

    val children = root.children()

    // Start the chain reaction.
    children.first().destroy()

    assertThat(root.isDestroyed()).isTrue()
    children.forEach { assertThat(it.isDestroyed()).isTrue() }
  }

  private class FakeScoped : Scoped {
    var onEnterCalled = 0
      private set

    var onExitCalled = 0
      private set

    override fun onEnterScope(scope: Scope) {
      onEnterCalled++
    }

    override fun onExitScope() {
      onExitCalled++
    }
  }
}
