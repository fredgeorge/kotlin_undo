package command

internal class CommandPrettyPrint(command: Undoable): CommandVisitor {
    private var result = ""
    private var indentCount = 0
    init {
        command.accept(this)
    }

    internal fun result() = result + "\n"

    override fun preVisit(command: Undoable, behavior: Undoable.Behavior?) {
        captureIdentifier(command)
    }

    override fun visit(behavior: Undoable.Behavior) {
        result += "  ".repeat(indentCount + 1) + "behavior: " + behavior.toString() + "\n"
    }

    override fun postVisit(command: Undoable, behavior: Undoable.Behavior?) {
        indentCount--
    }

    private fun captureIdentifier(command: Undoable) {
        result += "  ".repeat(indentCount) + command.identifier.toString() + "\n"
        indentCount++
    }
}