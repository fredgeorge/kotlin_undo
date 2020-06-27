/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package unit

import command.StatefulCommand
import command.Undoable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import visitor.CommandVisitor
import kotlin.test.assertFailsWith

internal class StatefulCommandTest {
    private lateinit var behavior: TestBehavior
    private lateinit var command: Undoable<Any>

    @Test
    fun `undone command can be re-done`() {
        command = TestBehavior { true }.command()
        assertCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)

        Assertions.assertTrue(command.undo())
        assertCounts(1, 1)

        assertEquals(true, command.execute())
        assertCounts(2, 1)
    }

    @Test
    fun `executing something that is done works, but does nothing new`() {
        command = TestBehavior { true }.command()
        assertCounts(0, 0, 0, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)

        assertEquals(true, command.execute())
        assertCounts(1, 0)
    }

    @Test
    fun `can resume a suspended command`() {
        command = TestBehavior { null }.command()
        assertCounts(0, 0, 0, 0)

        Assertions.assertNull(command.execute())
        assertCounts(0, 0, 0, 1)

        assertEquals(true, command.resume())
        assertCounts(1, 0, 0, 1)
    }

    @Test
    fun `a failed command will refuse to do anything except recover`() {
        command = TestBehavior { false }.command()
        assertCounts(0, 0, 0, 0)

        assertEquals(false, command.execute())
        assertCounts(0, 0, 1)

        assertFailsWith<IllegalStateException> { command.execute() }
        assertFailsWith<IllegalStateException> { command.resume() }
        Assertions.assertTrue(command.undo())
        assertCounts(0, 1, 1)
    }

    private fun assertCounts(
            expectedExecuteCount: Int = 0,
            expectedUndoCount: Int = 0,
            expectedAbortCount: Int = 0,
            expectedSuspendCount: Int = 0
    ) {
        assertEquals(expectedExecuteCount, behavior.executeCount)
        assertEquals(expectedUndoCount, behavior.undoCount)
        assertEquals(expectedAbortCount, behavior.abortCount)
        assertEquals(expectedSuspendCount, behavior.suspendCount)
    }

    private inner class TestBehavior(private val executeResult: () -> Boolean?): Undoable.Behavior<Any> {
        internal var executeCount = 0
        internal var abortCount = 0
        internal var undoCount = 0
        internal var suspendCount = 0

        internal fun command(): Undoable<Any> {
            behavior = this
            return StatefulCommand(behavior)
        }

        override fun executeAction(): Boolean? = executeResult.invoke()?.also { result ->
            if (result) executeCount++ else abortCount++
        } ?: null.also { suspendCount++ }

        override fun undoAction() = true.also { undoCount++ }

        override fun resumeAction(r: Any?) = true.also { executeCount++ }

        override fun accept(visitor: CommandVisitor<Any>) = visitor.visit(this)
    }
}