package software.ralf.app.platform.scope

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.Job

class DestructionTest {

  @Test
  fun `completed jobs are released`() {
    val firstJob = Job()
    val secondJob = Job()
    val destruction = Destruction(listOf(firstJob, secondJob), emptyList())

    assertThat(destruction.pendingJobCount()).isEqualTo(2)

    firstJob.complete()
    assertThat(destruction.pendingJobCount()).isEqualTo(1)

    secondJob.complete()
    assertThat(destruction.pendingJobCount()).isEqualTo(0)
  }
}
