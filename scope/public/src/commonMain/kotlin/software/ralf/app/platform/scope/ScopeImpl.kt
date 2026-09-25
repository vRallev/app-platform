package software.ralf.app.platform.scope

import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped

internal class ScopeImpl(
  override val name: String,
  private val parentScope: Scope?,
  private val services: Map<String, Any>,
) : Scope {

  private val scopedInstances = mutableSetOf<Scoped>()
  private var isDestroyed = false
  private var isDestroying = false

  private val children = mutableSetOf<ScopeImpl>()

  override val parent: Scope?
    get() {
      checkIsNotDestroyed()
      return parentScope
    }

  override fun buildChild(name: String, builder: (Scope.Builder.() -> Unit)?): Scope {
    checkIsNotDestroyed()
    return Scope.Builder(name, this)
      .apply {
        if (builder != null) {
          builder()
        }
      }
      .build()
      .also { children += it }
  }

  override fun children(): Set<Scope> {
    checkIsNotDestroyed()
    return children.toSet()
  }

  override fun register(scoped: Scoped) {
    checkIsNotDestroyed()

    if (scoped !in scopedInstances) {
      scopedInstances += scoped
      scoped.onEnterScope(this)
    }
  }

  override fun isDestroyed(): Boolean = isDestroyed

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getService(key: String): T? {
    checkIsNotDestroyed()
    return services[key] as T?
  }

  override fun destroy() {
    if (isDestroyed || isDestroying) return
    isDestroying = true

    // Heads up that destroying the child will modify the `children` set. By using a
    // sequence and computing the next value lazily this operation safe.
    generateSequence { children.firstOrNull { !it.isDestroying } }
      .forEach { childScope -> childScope.destroy() }

    // Cancel coroutines first to make the destruction of a scope more deterministic. A common
    // pattern is to use the CoroutineScope in onEnterScope() to launch several jobs. These
    // jobs often run in a loop. onExitScope() is used to release resources, to which the jobs
    // may react and run code to recreate these resources. This is problematic and requires
    // you to check in the jobs whether this scope (not the CoroutineScope) is destroyed.
    //
    // Instead of always checking whether the scope is destroyed, make sure to cancel all
    // coroutines first and then call onExitScope() on all other Scoped instances.
    scopedInstances.filterIsInstance<CoroutineScopeScoped>().forEach { it.onExitScope() }
    scopedInstances.filter { it !is CoroutineScopeScoped }.forEach { it.onExitScope() }

    // Release all references.
    scopedInstances.clear()

    (parent as? ScopeImpl)?.children?.remove(this)
    isDestroyed = true
    isDestroying = false
  }

  private fun checkIsNotDestroyed() {
    check(!isDestroyed) { "The scope $name is already destroyed." }
  }

  override fun toString(): String {
    return "Scope(name='$name', isDestroyed=$isDestroyed, children=${children.size}, " +
      "parentScope=$parentScope)"
  }
}
