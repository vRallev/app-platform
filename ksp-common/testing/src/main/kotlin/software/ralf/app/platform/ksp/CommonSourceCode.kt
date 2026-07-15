package software.ralf.app.platform.ksp

val Class<*>.inner: Class<*>
  get() = classes.single { it.simpleName == "Inner" }
