package unit

import command.SerialCompositeCommand
import command.StatefulCommand
import command.Undoable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SerialCompositeCommandTest {
    private lateinit var compositeBehavior: CompositeTestBehavior
    private lateinit var command: Undoable

    @Test
    fun `empty Composite cleanly completes`() {
        command = CompositeTestBehavior().command()
        assertCompositeCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCompositeCounts(1, 0, 1)

        assertTrue(command.undo())
        assertCompositeCounts(1, 1, 2)
    }

    @Test
    fun `SerialCompositeCommand with one sub-command`() {
        val step = TestBehavior { true }
        command = CompositeTestBehavior().command(step.command())
        assertCompositeCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCompositeCounts(1, 0, 1)
        assertStepCounts(step, 1, 0, 0, 0, 1)

        assertTrue(command.undo())
        assertCompositeCounts(1, 1, 2)
        assertStepCounts(step, 1, 1, 0, 0, 2)
    }

    @Test
    fun `SerialComposite with second step failing`() {
        val successfulStep = TestBehavior { true }
        val failingStep = TestBehavior { false }
        command = CompositeTestBehavior().command(
                successfulStep.command(),
                failingStep.command()
        )

        assertEquals(false, command.execute())
        assertCompositeCounts(1, 1, 1)
        assertStepCounts(successfulStep, 1, 1, 0, 0, 2)
        assertStepCounts(failingStep, 0, 0, 1, 0, 1)
    }

    private fun assertCompositeCounts(
            expectedExecuteCount: Int = 0,
            expectedUndoCount: Int = 0,
            expectedCleanupCount: Int = 0,
            expectedSuspendCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, compositeBehavior.executeCount)
        assertEquals(expectedUndoCount, compositeBehavior.undoCount)
        assertEquals(expectedCleanupCount, compositeBehavior.cleanupCount)
        assertEquals(expectedSuspendCount, compositeBehavior.suspendCount)
    }

    private fun assertStepCounts(
            stepBehavior: TestBehavior,
            expectedExecuteCount: Int = 0,
            expectedUndoCount: Int = 0,
            expectedAbortCount: Int = 0,
            expectedSuspendCount: Int = 0,
            expectedCleanupCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, stepBehavior.executeCount)
        assertEquals(expectedUndoCount, stepBehavior.undoCount)
        assertEquals(expectedAbortCount, stepBehavior.abortCount)
        assertEquals(expectedSuspendCount, stepBehavior.suspendCount)
        assertEquals(expectedCleanupCount, stepBehavior.cleanupCount)
    }

    private inner class CompositeTestBehavior : Undoable.Behavior {
        internal var executeCount = 0
        internal var undoCount = 0
        internal var cleanupCount = 0
        internal var suspendCount = 0

        internal fun command(vararg steps: Undoable): Undoable {
            compositeBehavior = this
            return SerialCompositeCommand(*steps, behavior = compositeBehavior)
        }

        override fun executeAction() = true.also { executeCount++ }

        override fun undoAction() = true.also { undoCount++ }

        override fun resumeAction() = true.also { executeCount++ }

        override fun cleanupAction()  { cleanupCount++ }
    }

    private inner class TestBehavior(private val executeResult: () -> Boolean?): Undoable.Behavior {
        internal var executeCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0
        internal var cleanupCount = 0

        internal fun command() = StatefulCommand(this)

        override fun executeAction(): Boolean? = executeResult.invoke()?.also { result ->
            if (result) executeCount++ else abortCount++
        } ?: null.also { suspendCount++ }

        override fun undoAction() = true.also { undoCount++ }

        override fun resumeAction() = true.also { executeCount++ }

        override fun cleanupAction() { cleanupCount++ }
    }
}