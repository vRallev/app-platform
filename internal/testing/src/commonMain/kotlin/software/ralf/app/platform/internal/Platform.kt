package software.ralf.app.platform.internal

/** All test environment targets. */
enum class Platform {
  /** The JVM target includes Android and Desktop. */
  JVM,
  /** The Native target includes Apple and Linux. */
  Native,
  /** The Web target includes Wasm. */
  Web,
}

/** The current test environment target. */
expect val platform: Platform
