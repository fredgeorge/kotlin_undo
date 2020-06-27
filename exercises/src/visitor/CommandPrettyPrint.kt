package visitor

import command.Undoable

internal class CommandPrettyPrint<R>(command: Undoable<R>): CommandVisitor<R> {
    private var result = ""
    private var indentCount = 0
    init {
        command.accept(this)
    }

    internal fun result() = result + "\n"

    override fun preVisit(command: Undoable<R>, behavior: Undoable.Behavior<R>?) {
        captureIdentifier(command)
    }

    override fun visit(behavior: Undoable.Behavior<R>) {
        result += "  ".repeat(indentCount + 1) + "behavior: " + behavior.toString() + "\n"
    }

    override fun postVisit(command: Undoable<R>, behavior: Undoable.Behavior<R>?) {
        indentCount--
    }

    private fun captureIdentifier(command: Undoable<R>) {
        result += "  ".repeat(indentCount) + command.identifier.toString() + "\n"
        indentCount++
    }
}