/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package visitor

import command.Undoable

internal class CommandPrettyPrint<R>(command: Undoable<R>): CommandVisitor<R> {
    private var result = ""
    private var indentCount = 0
    init {
        command.accept(this)
    }

    internal fun result() = result + "\n"

    override fun preVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>,
            behavior: Undoable.Behavior<R>?,
            status: Undoable.Status
    ) {
        result += "  ".repeat(indentCount) +
                        "${command.identifier} with ${steps.size} steps\n"
        indentCount++
    }

    override fun preVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>?,
            status: Undoable.Status
    ) {
        result += "  ".repeat(indentCount) + command.identifier.toString() + "\n"
        indentCount++
    }

    override fun visit(behavior: Undoable.Behavior<R>) {
        result += "  ".repeat(indentCount + 1) + "behavior: " + behavior.toString() + "\n"
    }

    override fun postVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>?,
            status: Undoable.Status
    ) {
        indentCount--
    }

    override fun postVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>,
            behavior: Undoable.Behavior<R>?,
            status: Undoable.Status
    ) {
        indentCount--
    }
}