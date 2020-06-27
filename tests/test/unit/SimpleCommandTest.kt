/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package unit

import command.Undoable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import visitor.CommandVisitor

internal class SimpleCommandTest {
    private lateinit var command: SimpleTestCommand

    @Test
    fun `simple positive command actions`() {
        command = SimpleTestCommand { true }
        assertCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)

        assertTrue(command.undo())
        assertCounts(1, 1)
    }

    @Test
    fun `simple negative command actions`() {
        command = SimpleTestCommand { false }
        assertCounts(0, 0, 0, 0)

        assertEquals(false, command.execute())
        assertCounts(0, 0, 1)
    }

    @Test
    fun `simple suspension`() {
        command = SimpleTestCommand { null }
        assertCounts(0, 0, 0, 0)

        assertNull(command.execute())
        assertCounts(expectedSuspendCount = 1)

        assertEquals(true, command.resume())
        assertCounts(1, 0, 0, 1)
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

    private class SimpleTestCommand(private val executeAction: () -> Boolean?): Undoable<Any> {
        internal var executeCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0
        override val identifier = "SimpleTestCommand in ${this.javaClass.simpleName}"

        override fun execute() = executeAction()?.also { result ->
            if (result) executeCount++
            else abortCount++
        } ?: null.also{ suspendCount ++ }

        override fun undo(): Boolean {
            undoCount++
            return true
        }

        override fun resume(r: Any?): Boolean? {
            executeCount++
            return true
        }

        override fun accept(visitor: CommandVisitor<Any>) {}     // Ignore
    }
}