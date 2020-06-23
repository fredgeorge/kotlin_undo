/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

class StatefulCommand (
        private var behavior: Undoable.Behavior,
        override val identifier: Any = "<unidentified StatefulCommand>"
): Undoable {

    private var state: ExecutionState = Ready()

    override fun execute() = state.execute { behavior.executeAction() }

    override fun undo() = state.undo { behavior.undoAction() }

    override fun resume() = state.resume { behavior.resumeAction() }

    override fun accept(visitor: CommandVisitor) {
        visitor.preVisit(this, behavior)
        behavior.accept(visitor)
        visitor.postVisit(this, behavior)
    }

    override fun inject(behavior: Undoable.Behavior) {
        this.behavior = behavior
    }

    override fun toString() = CommandPrettyPrint(this).result()

    private fun process(sideEffect: () -> Boolean?) = sideEffect()?.also { result ->
        state = if (result) Success() else Failure()
        behavior.cleanupAction()
    } ?: null.also {
        state = Suspended()
    }

    private fun reverse(sideEffect: () -> Boolean) = sideEffect().also { result ->
        state = if (result) Ready() else Failure()
        behavior.cleanupAction()
    }

    private interface ExecutionState {
        fun execute(sideEffect: () -> Boolean?): Boolean?
        fun undo(sideEffect: () -> Boolean): Boolean
        fun resume(sideEffect: () -> Boolean?): Boolean?
    }

    private inner class Ready: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = process(sideEffect)

        override fun undo(sideEffect: () -> Boolean) = true  // Ignore since nothing to undo

        override fun resume(sideEffect: () -> Boolean?): Boolean? {
            throw IllegalStateException("Trying to resume when not suspended")
        }
    }

    private inner class Success: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = true  // Idempotent response

        override fun undo(sideEffect: () -> Boolean) = reverse(sideEffect)

        override fun resume(sideEffect: () -> Boolean?) = true  // Idempotent response, and would support chaining of resume()
    }

    private inner class Suspended: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?) = resume(sideEffect)  // States can allow this to be re-interpreted as resume

        override fun undo(sideEffect: () -> Boolean) = reverse(sideEffect)

        override fun resume(sideEffect: () -> Boolean?) = process(sideEffect)
    }

    private inner class Failure: ExecutionState {

        override fun execute(sideEffect: () -> Boolean?): Boolean? {
            throw IllegalStateException("Command has already failed")
        }

        override fun undo(sideEffect: () -> Boolean) = sideEffect().also { result ->
            if (!result) throw IllegalStateException("Command unable to recover from failure")
            state = Ready()
            behavior.cleanupAction()
        }

        override fun resume(sideEffect: () -> Boolean?): Boolean? {
            throw IllegalStateException("Command has already failed")
        }

    }
}