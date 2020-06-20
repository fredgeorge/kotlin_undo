package unit

import command.SerialCompositeCommand
import command.StatefulCommand
import command.Undoable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SerialCompositeCommandTest {
    private lateinit var compositeBehavior: CompositeTestBehavior
    private lateinit var command: Undoable

    @Test
    fun `empty Composite behaves correctly`() {
        command = CompositeTestBehavior().command()
        assertCompositeCounts(0, 0, 0)

        assertEquals(true, command.execute())
        assertCompositeCounts(1, 0, 1)

        assertTrue(command.undo())
        assertCompositeCounts(1, 1, 2)
    }

    @Test
    fun `SerialCompositeCommand with one sub-command`() {
        val step = TestBehavior { true }
        command = CompositeTestBehavior().commandWithBehaviors(step)
        assertCompositeCounts(0, 0, 0)

        assertEquals(true, command.execute())
        assertCompositeCounts(1, 0, 1)
        assertStepCounts(
                stepBehavior = step,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )

        assertTrue(command.undo())
        assertCompositeCounts(1, 1, 2)
        assertStepCounts(
                stepBehavior = step,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedUndoCount = 1,
                expectedCleanupCount = 2
        )
    }

    @Test
    fun `SerialComposite with second step failing`() {
        val successfulStep = TestBehavior { true }
        val failingStep = TestBehavior { false }
        command = CompositeTestBehavior().commandWithBehaviors(
                successfulStep,
                failingStep
        )

        assertEquals(false, command.execute())
        assertCompositeCounts(1, 1, 1)
        assertStepCounts(
                stepBehavior = successfulStep,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedUndoCount = 1,
                expectedCleanupCount = 2
        )
        assertStepCounts(
                stepBehavior = failingStep,
                expectedExecuteCount = 1,
                expectedAbortCount = 1,
                expectedCleanupCount = 1
        )
    }

    @Test
    fun `can undo a completed SerialCompositeCommand`() {
        val step1Success = TestBehavior { true }
        val step2Success = TestBehavior { true }
        command = CompositeTestBehavior().commandWithBehaviors(
                step1Success,
                step2Success
        )

        assertEquals(true, command.execute())
        assertCompositeCounts(1, 0, 1)
        assertStepCounts(
                stepBehavior = step1Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )
        assertStepCounts(
                stepBehavior = step2Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )

        assertTrue(command.undo())
        assertCompositeCounts(1, 1, 2)
        assertStepCounts(
                stepBehavior = step1Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedUndoCount = 1,
                expectedCleanupCount = 2
        )
        assertStepCounts(
                stepBehavior = step2Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedUndoCount = 1,
                expectedCleanupCount = 2
        )
    }

    @Test
    fun `can resume a suspended sub-command`() {
        val step1Success = TestBehavior { true }
        val step2Suspend = TestBehavior { null }
        val step3Success = TestBehavior { true }
        command = CompositeTestBehavior().commandWithBehaviors(
                step1Success,
                step2Suspend,
                step3Success
        )

        assertNull(command.execute())
        assertCompositeCounts(1, 0, 0)
        assertStepCounts(
                stepBehavior = step1Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )
        assertStepCounts(
                stepBehavior = step2Suspend,
                expectedExecuteCount = 1,
                expectedSuspendCount = 1
        )
        assertStepCounts(step3Success)

        assertEquals(true, command.resume())
        assertCompositeCounts(1, 0, 1)
        assertStepCounts(
                stepBehavior = step1Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )
        assertStepCounts(
                stepBehavior = step2Suspend,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedSuspendCount = 1,
                expectedResumeCount = 1,
                expectedCleanupCount = 1
        )
        assertStepCounts(
                stepBehavior = step3Success,
                expectedExecuteCount = 1,
                expectedSuccessCount = 1,
                expectedCleanupCount = 1
        )
    }

    @Test
    fun `3-tier composite with failing step`() {

    }

    private fun assertCompositeCounts(
            expectedExecuteCount: Int = 0,
            expectedUndoCount: Int = 0,
            expectedCleanupCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, compositeBehavior.executeCount, "Composite execute count")
        assertEquals(expectedUndoCount, compositeBehavior.undoCount, "Composite undo count")
        assertEquals(expectedCleanupCount, compositeBehavior.cleanupCount, "Composite cleanup count")
    }

    private fun assertStepCounts(
            stepBehavior: TestBehavior,
            expectedExecuteCount: Int = 0,
            expectedSuccessCount: Int = 0,
            expectedAbortCount: Int = 0,
            expectedUndoCount: Int = 0,
            expectedSuspendCount: Int = 0,
            expectedResumeCount: Int = 0,
            expectedCleanupCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, stepBehavior.executeCount, "Step execute count")
        assertEquals(expectedSuccessCount, stepBehavior.successCount, "Step success count")
        assertEquals(expectedAbortCount, stepBehavior.abortCount, "Step abort count")
        assertEquals(expectedUndoCount, stepBehavior.undoCount, "Step undo count")
        assertEquals(expectedSuspendCount, stepBehavior.suspendCount, "Step suspend count")
        assertEquals(expectedResumeCount, stepBehavior.resumeCount, "Step resume count")
        assertEquals(expectedCleanupCount, stepBehavior.cleanupCount, "Step cleanup count")
    }

    private inner class CompositeTestBehavior : Undoable.Behavior {
        internal var executeCount = 0
        internal var undoCount = 0
        internal var cleanupCount = 0

        internal fun command(vararg steps: Undoable): Undoable {
            compositeBehavior = this
            return SerialCompositeCommand(*steps, behavior = compositeBehavior)
        }

        internal fun commandWithBehaviors(vararg behaviors: TestBehavior): Undoable =
                command(*(behaviors.map { it.command() }.toTypedArray()))

        override fun executeAction() = true.also { executeCount++ }

        override fun undoAction() = true.also { undoCount++ }

        override fun resumeAction() = true.also { executeCount++ }

        override fun cleanupAction()  { cleanupCount++ }
    }

    private inner class TestBehavior(private val executeResult: () -> Boolean?): Undoable.Behavior {
        internal var executeCount = 0
        internal var successCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0
        internal var resumeCount = 0
        internal var cleanupCount = 0

        internal fun command() = StatefulCommand(this)

        override fun executeAction(): Boolean? = executeResult.invoke()?.also { result ->
            executeCount++
            if (result) successCount++ else abortCount++
        } ?: null.also { executeCount++; suspendCount++ }

        override fun undoAction() = true.also { undoCount++ }

        override fun resumeAction() = true.also { resumeCount++; successCount++ }

        override fun cleanupAction() { cleanupCount++ }
    }
}