package software.ralf.app.platform.listdetail

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Instrumentation runner that replaces the production application with [TestAndroidApplication].
 */
@Suppress("unused")
class TestRunner : AndroidJUnitRunner() {
  override fun newApplication(
    classLoader: ClassLoader,
    className: String,
    context: Context,
  ): Application {
    return newApplication(TestAndroidApplication::class.java, context)
  }
}
