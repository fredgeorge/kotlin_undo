package unit

import command.Undoable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import kotlin.test.assertFailsWith

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

    private class StatefulTestCommand(private val executeAction: () -> Boolean?): Undoable {
        internal var executeCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0

        private var state: ExecutionState = Ready()

        override fun execute() = state.execute()

        override fun undo() = state.undo()

        override fun resume() = state.resume()

        private interface ExecutionState: Undoable

        private inner class Ready: ExecutionState {

            override fun execute() = executeAction()?.also { result ->
                state = if (result) {
                    executeCount++
                    Success()
                } else {
                    abortCount++
                    Failure()
                }
            } ?: null.also {
                suspendCount++
                state = Suspended()
            }

            override fun undo() = true  // Ignore

            override fun resume(): Boolean? { throw IllegalStateException("Trying to resume when not suspended") }

        }

        private inner class Success: ExecutionState {

            override fun execute() = true  // Idempotent response

            override fun undo(): Boolean {
                undoCount += 1
                state = Ready()
                return true            // If undo unsuccessful, consider moving to Failure state
            }

            override fun resume(): Boolean? { throw IllegalStateException("Trying to resume when not suspended") }
        }

        private inner class Suspended: ExecutionState {

            override fun execute() = resume()  // States can allow this to be re-interpreted as resume

            override fun resume(): Boolean? {
                executeCount++
                return true
            }

            override fun undo(): Boolean {
                abortCount += 1
                undoCount += 1
                return true
            }
        }

        private class Failure: ExecutionState {

            override fun execute(): Boolean? { throw IllegalStateException("Command has already failed") }

            override fun undo(): Boolean { throw IllegalStateException("Command has already failed") }

            override fun resume(): Boolean? { throw IllegalStateException("Command has already failed") }
        }
    }
}