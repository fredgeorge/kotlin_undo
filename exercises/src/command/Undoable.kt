package command

interface Undoable {
    fun execute(): Boolean?  // Null indicates suspended execution
    fun undo(): Boolean
    fun resume() = execute()
}