package unit

import command.Undoable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import kotlin.test.assertFailsWith
import command.StatefulCommand as StatefulCommand

internal class ExecuteOnceTest {
    private lateinit var command: StatefulTestCommand

    @Test
    fun `undone command can be re-done`() {
        command = StatefulTestCommand { true }
        assertCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)

        assertTrue(command.undo())
        assertCounts(1, 1)

        assertEquals(true, command.execute())
        assertCounts(2, 1)
    }

    @Test
    fun `executing something that is done works, but does nothing new`() {
        command = StatefulTestCommand { true }
        assertCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)
    }

    @Test
    fun `can resume a suspended command`() {
        command = StatefulTestCommand { null }
        assertCounts(0, 0, 0, 0)

        assertNull(command.execute())
        assertCounts(0, 0, 0, 1)

        assertEquals(true, command.resume())
        assertCounts(1, 0, 0, 1)
    }

    @Test
    fun `a failed command will refuse to do anything`() {
        command = StatefulTestCommand { false }
        assertCounts(0, 0, 0, 0)

        assertEquals(false, command.execute())
        assertCounts(0, 0, 1)

        assertFailsWith<IllegalStateException> { command.execute() }
        assertFailsWith<IllegalStateException> { command.resume() }
        assertFailsWith<IllegalStateException> { command.undo() }
    }

    private fun assertCounts(
        expectedExecuteCount: Int = 0,
        expectedUndoCount: Int = 0,
        expectedAbortCount: Int = 0,
        expectedSuspendCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, command.executeCount)
        assertEquals(expectedUndoCount, command.undoCount)
        assertEquals(expectedAbortCount, command.abortCount)
        assertEquals(expectedSuspendCount, command.suspendCount)
    }

    private class StatefulTestCommand(private val executeResult: () -> Boolean?): Undoable {
        internal var executeCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0

        private val command = StatefulCommand(
                { this.executeAction() },
                { this.undoAction() },
                { this.resumeAction() }
        )

        override fun execute() = command.execute()
        override fun undo() = command.undo()
        override fun resume() = command.resume()

        private fun executeAction(): Boolean? = executeResult.invoke()?.also { result ->
            if (result) executeCount++ else abortCount++
        } ?: null.also { suspendCount++ }

        private fun undoAction() = true.also { undoCount++ }

        private fun resumeAction() = true.also { executeCount++ }
    }
}