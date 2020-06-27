package decorator

import command.Undoable
import visitor.CommandVisitor
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

class ActionTracer<R>(command: Undoable<R>): CommandVisitor<R> {
    private var result = ""

    init {
        command.accept(this)
    }

    override fun preVisit(command: Undoable<R>, behavior: Undoable.Behavior<R>?) {
        behavior?.also { command.inject(Trace(command.identifier, behavior)) }
    }

    fun result() = result

    inner class Trace<R>(
            private val indentifier: Any,
            private val baseBehavior: Undoable.Behavior<R>
    ): Undoable.Trace<R> {

        override fun executeAction(): Boolean? {
            log0(::executeAction)
            return baseBehavior.executeAction()
        }

        override fun undoAction(): Boolean {
            log0(::undoAction)
            return baseBehavior.undoAction()
        }

        override fun resumeAction(r: R?): Boolean? {
            log1(::resumeAction)
            return baseBehavior.resumeAction()
        }

        override fun cleanupAction() {
            log0(::cleanupAction)
            return baseBehavior.cleanupAction()
        }

        private fun log0(kFunction0: KFunction0<Any?>) {
            result += "${kFunction0.name} invoked for ${indentifier.toString()}\n"
        }

        private fun log1(kFunction1: KFunction1<R?, Boolean?>) {
            result += "${kFunction1.name} invoked for ${indentifier.toString()}\n"
        }
    }
}