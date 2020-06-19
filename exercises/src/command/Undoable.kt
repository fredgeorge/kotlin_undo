package command

interface Undoable<T> {
    fun execute(t: T): Boolean?  // Null indicates suspended execution
    fun undo(t: T): Boolean
    fun resume(t: T) = execute(t)
}