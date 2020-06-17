package command

import java.lang.IllegalStateException

class StatefulCommand(
        private val executeAction: () -> Boolean?,
        private val undoAction: () -> Boolean = { true },
        private val resumeAction: () -> Boolean? = executeAction
): Undoable {

    private var state: ExecutionState = Ready()

    private fun defaultUndoAction(): Boolean = true

    override fun execute() = state.execute()

    override fun undo() = state.undo()

    override fun resume() = state.resume()

    private interface ExecutionState: Undoable

    private inner class Ready: ExecutionState {

        override fun execute() = executeAction()?.also { result ->
            state = if (result) {
                Success()
            } else {
                Failure()
            }
        } ?: null.also {
            state = Suspended()
        }

        override fun undo() = true  // Ignore

        override fun resume(): Boolean? { throw IllegalStateException("Trying to resume when not suspended") }

    }

    private inner class Success: ExecutionState {

        override fun execute() = true  // Idempotent response

        override fun undo() = undoAction().also { result ->
            state = if (result) Ready() else Failure()
        }

        override fun resume(): Boolean? { throw IllegalStateException("Trying to resume when not suspended") }
    }

    private inner class Suspended: ExecutionState {

        override fun execute() = resume()  // States can allow this to be re-interpreted as resume

        override fun resume() = resumeAction()?.also { result ->
            state = if (result) Success() else Failure()
        } ?: null.also {
            state = Suspended()
        }

        override fun undo() = undoAction().also { result ->
            state = if (result) Ready() else Failure()
        }
    }

    private class Failure: ExecutionState {

        override fun execute(): Boolean? { throw IllegalStateException("Command has already failed") }

        override fun undo(): Boolean { throw IllegalStateException("Command has already failed") }

        override fun resume(): Boolean? { throw IllegalStateException("Command has already failed") }
    }
}