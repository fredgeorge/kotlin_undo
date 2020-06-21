package command

internal class CommandPrettyPrint(command: Undoable): CommandVisitor {
    private var result = ""
    private var indentCount = 0
    init {
        command.accept(this)
    }

    internal fun result() = result + "\n"

    override fun preVisit(command: SerialCompositeCommand) {
        captureIdentifier(command)
    }

    override fun preVisit(command: StatefulCommand) {
        captureIdentifier(command)
    }

    override fun preVisit(command: Undoable) {
        captureIdentifier(command)
    }

    override fun visit(behavior: Undoable.Behavior) {
        result += "  ".repeat(indentCount + 1) + "behavior: " + behavior.toString() + "\n"
    }

    override fun postVisit(command: Undoable) {
        indentCount--
    }

    override fun postVisit(command: StatefulCommand) {
        indentCount--
    }

    override fun postVisit(command: SerialCompositeCommand) {
        indentCount--
    }

    private fun captureIdentifier(command: Undoable) {
        result += "  ".repeat(indentCount) + command.identifier.toString() + "\n"
        indentCount++
    }
}