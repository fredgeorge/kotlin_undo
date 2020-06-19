package command

import java.lang.IllegalStateException
import kotlin.reflect.KFunction1

class StatefulCommand <T>(
        private val executeAction: KFunction1<T, Boolean?>,
        private val undoAction: KFunction1<T, Boolean>,
        private val resumeAction: KFunction1<T, Boolean?> = executeAction
): Undoable<T> {

    private var state: ExecutionState = Ready()

    override fun execute(t: T) = state.execute { executeAction(t) }

    override fun undo(t: T) = state.undo { undoAction(t) }

    override fun resume(t: T) = state.resume { resumeAction(t) }

    private interface ExecutionState {
        fun execute(sideEffect: () -> Boolean?): Boolean?
        fun undo(sideEffect: () -> Boolean): Boolean
        fun resume(sideEffect: () -> Boolean?): Boolean?
    }

    private inner class Ready: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = sideEffect()?.also { result ->
            state = if (result) Success() else Failure()
        } ?: null.also {
            state = Suspended()
        }

        override fun undo(sideEffect: () -> Boolean) = true  // Ignore

        override fun resume(sideEffect: () -> Boolean?): Boolean? { throw IllegalStateException("Trying to resume when not suspended") }
    }

    private inner class Success: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = true  // Idempotent response

        override fun undo(sideEffect: () -> Boolean) = sideEffect().also { result ->
            state = if (result) Ready() else Failure()
        }

        override fun resume(sideEffect: () -> Boolean?) = true  // Idempotent response, and would support chaining of resume()
    }

    private inner class Suspended: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = resume(sideEffect)  // States can allow this to be re-interpreted as resume

        override fun undo(sideEffect: () -> Boolean) = sideEffect().also { result ->
            state = if (result) Ready() else Failure()
        }

        override fun resume(sideEffect: () -> Boolean?) = sideEffect()?.also { result ->
            state = if (result) Success() else Failure()
        } ?: null.also {
            state = Suspended()
        }
    }

    private class Failure: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?): Boolean? { throw IllegalStateException("Command has already failed") }

        override fun undo(sideEffect: () -> Boolean): Boolean { throw IllegalStateException("Command has already failed") }

        override fun resume(sideEffect: () -> Boolean?): Boolean? { throw IllegalStateException("Command has already failed") }

    }
}