package software.ralf.app.platform.scope

/**
 * Scopes define the boundary our software components operate in. A scope is a space with a
 * well-defined lifecycle that can be created and torn down. Scopes host other service objects and
 * serve as container for them, e.g. DI components. To receive a callback when a scope is created or
 * destroyed the [Scoped] interface can be used.
 *
 * Scopes can have 0-N children. Child scopes have the same or a shorter lifecycle as their parent
 * scope. If the parent scope gets destroyed, then all child scopes are destroyed as well.
 *
 * After a scope has been destroyed it should no longer be used. All methods will throw an exception
 * instead.
 */
public interface Scope {

  /** The name to identify this scope. */
  public val name: String

  /**
   * Returns the parent scope if this is a child scope and was created with [buildChild], or returns
   * `null` for the root scope that was created with [buildRootScope].
   */
  public val parent: Scope?

  /** Creates a new child scope with this scope as parent. */
  public fun buildChild(name: String, builder: (Builder.() -> Unit)? = null): Scope

  /** All child scopes of this scope. */
  public fun children(): Set<Scope>

  /**
   * Registers [scoped] to be notified when this scope is destroyed. Since this scope has been
   * already created at this point in time, [Scoped.onEnterScope] will be called immediately.
   */
  public fun register(scoped: Scoped)

  /**
   * Returns whether this scope has been destroyed. If `true`, then no other methods should be
   * called anymore.
   */
  public fun isDestroyed(): Boolean

  /**
   * Destroy this scope and all its children. Calling this function will invoke [Scoped.onExitScope]
   * for all registered [Scoped] instances.
   */
  public fun destroy()

  /**
   * Returns a registered service for the given [key] or null if no service is registered with this
   * key.
   */
  public fun <T : Any> getService(key: String): T?

  /** Builder type to construct a new [Scope] instance. */
  public class Builder internal constructor(private val name: String, private val parent: Scope?) {
    private val services = mutableMapOf<String, Any>()
    private val scopedInstances = mutableSetOf<Scoped>()

    /** Adds the given [service] to the scope that is about to be constructed. */
    public fun addService(key: String, service: Any) {
      services[key] = service
    }

    /** Registers [scoped] to be notified when this scope is created and destroyed. */
    public fun register(scoped: Scoped) {
      scopedInstances += scoped
    }

    internal fun build(): ScopeImpl {
      return ScopeImpl(name, parent, services).apply { register(scopedInstances) }
    }
  }

  public companion object {
    /** Builds a scope without any parent. */
    public fun buildRootScope(name: String = "root", builder: (Builder.() -> Unit)? = null): Scope =
      Builder(name, null)
        .apply {
          if (builder != null) {
            builder()
          }
        }
        .build()
  }
}

/**
 * Returns a chain of scopes beginning with this scope if [includeSelf] is `true` or the
 * [Scope.parent] if [includeSelf] is false followed by each parent of the previous scope until the
 * root scope is reached. The returned sequence is empty for root scope and [includeSelf] being
 * `false`.
 */
public fun Scope.parents(includeSelf: Boolean = false): Sequence<Scope> =
  generateSequence(this) { it.parent }.drop(if (includeSelf) 0 else 1)

/**
 * Registers [scopedInstances] to be notified when this scope is destroyed. Since this scope has
 * been already created at this point in time, [Scoped.onEnterScope] will be called immediately for
 * all instance.
 */
public fun Scope.register(scopedInstances: Iterable<Scoped>) {
  scopedInstances.forEach { register(it) }
}

/** Registers [scopedInstances] to be notified when this scope is created and destroyed. */
public fun Scope.Builder.register(scopedInstances: Iterable<Scoped>) {
  scopedInstances.forEach { register(it) }
}

/**
 * Invokes the given lambda when the scope is destroyed. This is a convenience function when
 * [Scoped.onExitScope] cannot be overridden or fields are tedious to manage, e.g.
 *
 * ```
 * override fun onEnterScope(scope: Scope) {
 *     val receiver = BroadCastReceiver()
 *
 *     application.registerReceiver(receiver)
 *
 *     scope.onExit {
 *         application.unregisterReceiver(receiver)
 *     }
 * }
 * ```
 */
public fun Scope.onExit(block: () -> Unit) {
  register(
    object : Scoped {
      override fun onExitScope() {
        block()
      }
    }
  )
}
