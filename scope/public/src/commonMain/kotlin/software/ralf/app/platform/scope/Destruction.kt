package software.ralf.app.platform.scope

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

internal class Destruction(jobs: List<Job>, childDestructions: List<Destruction>) {

  private val lock = SynchronizedObject()
  private val completionJob: CompletableJob = Job()
  private var pendingJobs = jobs + childDestructions.map { it.completionJob }

  init {
    pendingJobs.forEach { job -> job.invokeOnCompletion { clearCompletedJobs() } }
    clearCompletedJobs()
  }

  suspend fun await() {
    completionJob.join()
  }

  internal fun pendingJobCount(): Int = synchronized(lock) { pendingJobs.size }

  private fun clearCompletedJobs() {
    val isComplete =
      synchronized(lock) {
        pendingJobs = pendingJobs.filterNot { it.isCompleted }
        pendingJobs.isEmpty()
      }

    if (isComplete) {
      completionJob.complete()
    }
  }
}
