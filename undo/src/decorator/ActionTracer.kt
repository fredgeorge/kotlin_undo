/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package decorator

import command.Undoable
import visitor.CommandVisitor
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

// Understands all the Behaviors invoked for a Command hierarchy
// Visitor pattern [GoF] used to inject an intercepting Behavior
class ActionTracer<R>(command: Undoable<R>): CommandVisitor<R> {
    private var result = ""

    init {
        command.accept(this)
    }

    override fun preVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>?,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        injectTracer(command, behavior)
    }

    override fun preVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        injectTracer(command, behavior)
    }

    private fun injectTracer(command: Undoable<R>, behavior: Undoable.Behavior<R>) {
        command.inject(Trace(command.identifier, behavior))
    }

    fun result() = result

    // Tracks the invocation of a single Behavior
    // Decorator pattern [GoF] used to intercept, then relay, all Behavior APIs
    inner class Trace<R>(
            private val indentifier: Any,
            private val baseBehavior: Undoable.Behavior<R>
    ): Undoable.Trace<R> {

        private var actionCount = 0

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

        override fun accept(visitor: CommandVisitor<R>) {
            visitor.visit(this)
            baseBehavior.accept(visitor)
        }

        override fun toString() = "actions traced: $actionCount"

        private fun log0(kFunction0: KFunction0<Any?>) = log(kFunction0.name)

        private fun log1(kFunction1: KFunction1<R?, Boolean?>) = log(kFunction1.name)

        private fun log(actionName: String) {
            actionCount++
            result += "$actionName invoked for ${indentifier.toString()}\n"
        }
    }
}