package command

import kotlin.reflect.KFunction0

class ActionTracer(command: Undoable): CommandVisitor {
    private var result = ""

    init {
        command.accept(this)
    }

    override fun preVisit(command: Undoable, behavior: Undoable.Behavior?) {
        behavior?.also { command.inject(Trace(command.identifier, behavior)) }
    }

    fun result() = result

    inner class Trace(
            private val indentifier: Any,
            private val baseBehavior: Undoable.Behavior
    ): Undoable.Trace {

        override fun executeAction(): Boolean? {
            log(::executeAction)
            return baseBehavior.executeAction()
        }

        override fun undoAction(): Boolean {
            log(::undoAction)
            return baseBehavior.undoAction()
        }

        override fun resumeAction(): Boolean? {
            log(::resumeAction)
            return baseBehavior.resumeAction()
        }

        override fun cleanupAction() {
            log(::cleanupAction)
            return baseBehavior.cleanupAction()
        }

        private fun log(kFunction0: KFunction0<Any?>) {
            result += "${kFunction0.name} invoked for ${indentifier.toString()}\n"
        }
    }
}