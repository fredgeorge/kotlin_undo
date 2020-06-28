/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package unit

import command.JsonPersistence
import command.SerialCompositeCommand
import command.StatefulCommand
import command.Undoable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import visitor.CommandVisitor

internal class JsonPersistenceTest {

    @Test
    fun `can render multi-tier Commands`() {
        val step1ASuccess = TestBehavior { true }
        val step1BSuccess = TestBehavior { true }
        val step2ASuccess = TestBehavior { true }
        val step2BSuccess = TestBehavior { true }
        val step2CSuccess = TestBehavior { true }
        val command1 = TestBehavior().command(
                step1ASuccess.command("step1ASuccess"),
                step1BSuccess.command("step1BSuccess"),
                label = "SubCommand 1"
        )
        val command2 = TestBehavior().command(
                step2ASuccess.command("step2ASuccess"),
                step2BSuccess.command("step2BSuspend"),
                step2CSuccess.command("step2CSuccess"),
                label = "SubCommand 2"
        )
        val command = TestBehavior().command(command1, command2, label = "Main Command").also {
            it.trace()
        }
        assertEquals(true, command.execute())

        println(JsonPersistence(command))
    }

    private inner class TestBehavior(
            private val executeResult: () -> Boolean? = { true }
    ): Undoable.Behavior<String> {

        internal fun command(label: String = "<unknown step>") = StatefulCommand(this, identifier = label)

        internal fun command(
                vararg steps: Undoable<String>,
                label: String = "<unknown step>"
        ) = SerialCompositeCommand(*steps, behavior = this, identifier = label)

        override fun executeAction(): Boolean? = executeResult.invoke()
        override fun undoAction() = true
        override fun resumeAction(r: String?) = true
        override fun accept(visitor: CommandVisitor<String>) = visitor.visit(this)
    }
}