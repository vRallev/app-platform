package software.ralf.app.platform.sample

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** The test runner overrides the application class in favor of the test version. */
@Suppress("unused")
class TestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
    newApplication(TestAndroidApplication::class.java, context)
}
