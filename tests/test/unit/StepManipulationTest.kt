package unit

import command.SerialCompositeCommand
import command.Undoable
import command.Undoable.Status
import command.Undoable.Status.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import visitor.CommandVisitor

internal class StepManipulationTest {
    private lateinit var command: SerialCompositeCommand<String>
    private val inspector get() = CommandInspector(command)

    @Test
    fun `add step to existing composite command`() {
        command = SerialCompositeCommand(TestStep { true }, TestStep { true })
        assertEquals(2, inspector.stepCount)
        command.add(TestStep { true })
        assertEquals(3, inspector.stepCount)
    }

    @Test
    fun `remove a step from existing command`() {
        val step1 = TestStep { true }
        val step2 = TestStep { true }
        val step3 = TestStep { true }
        command = SerialCompositeCommand(step1, step2, step3)

        assertEquals(3, inspector.stepCount)

        command.remove(step2)
        inspector.also { inspector ->
            assertEquals(2, inspector.stepCount)
            assertEquals(step1, inspector.currentStep)
        }

        assertEquals(true, command.execute())
        inspector.also { inspector ->
            assertEquals(2, inspector.stepCount)
            assertEquals(step3, inspector.currentStep)
            assertEquals(Complete, step1.status())
            assertEquals(Ready, step2.status())
            assertEquals(Complete, step3.status())
        }
    }

    @Test
    fun `remove first step from ready command`() {
        val step1 = TestStep { true }
        val step2 = TestStep { true }
        command = SerialCompositeCommand(step1, step2)

        assertEquals(2, inspector.stepCount)
        assertEquals(step1, inspector.currentStep)

        command.remove(step1)
        inspector.also { inspector ->
            assertEquals(1, inspector.stepCount)
            assertEquals(step2, inspector.currentStep)
        }
    }

    @Test
    fun `cannot remove a pending step`() {
        val step1 = TestStep { true }
        val step2 = TestStep { null }
        command = SerialCompositeCommand(step1, step2)

        assertEquals(2, inspector.stepCount)
        assertEquals(step1, inspector.currentStep)

        assertNull(command.execute())
        assertEquals(Complete, step1.status())
        assertEquals(Pending, step2.status())

        assertThrows<IllegalStateException> { command.remove(step2) } // It's pending

        assertEquals(true, command.resume("anything"))
        inspector.also { inspector ->
            assertEquals(2, inspector.stepCount)
            assertEquals(step2, inspector.currentStep)
        }

        command.remove(step2)  // No longer pending
        inspector.also { inspector ->
            assertEquals(1, inspector.stepCount)
            assertEquals(step1, inspector.currentStep)
        }
    }

    @Test
    fun `remove only step from ready command`() {
        val step1 = TestStep { true }
        command = SerialCompositeCommand(step1)

        assertEquals(1, inspector.stepCount)
        assertEquals(step1, inspector.currentStep)

        command.remove(step1)
        inspector.also { inspector ->
            assertEquals(0, inspector.stepCount)  // Now has invisible NullStep
            assertNull(inspector.currentStep)
        }
    }

    private class CommandInspector(command: SerialCompositeCommand<String>) : CommandVisitor<String> {
        internal var currentStep: Undoable<String>? = null
        internal var stepCount = 0

        init {
            command.accept(this)
        }

        override fun preVisit(
                command: Undoable.Composite<String>,
                steps: List<Undoable<String>>,
                currentStep: Undoable<String>?,
                behavior: Undoable.Behavior<String>,
                status: Status
        ) {
            this.currentStep = currentStep
            stepCount = 0
        }

        override fun preVisit(
                command: Undoable<String>,
                behavior: Undoable.Behavior<String>,
                status: Status
        ) {
            stepCount++
        }
    }

    private class TestStep(private val executeAction: () -> Boolean?): Undoable<String> {
        private var status: Status = Ready
        override val identifier = "Step $stepIndex"

        companion object {
            private var stepIndex = 1
        }

        init {
            stepIndex++
        }

        override fun execute(): Boolean? = executeAction().also { result ->
            status = when (result) {
                true -> Complete
                false -> Failure
                else -> Pending
            }
        }

        override fun undo() = true.also { status = Ready }

        override fun resume(r: String?) = true.also { status = Complete }

        override fun status() = status

        override fun accept(visitor: CommandVisitor<String>) {
            visitor.preVisit(this, Undoable.NoBehavior(), status)
            visitor.postVisit(this, Undoable.NoBehavior(), status)
        }

        override fun toString() = identifier
    }
}