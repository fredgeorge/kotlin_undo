package command

interface Undoable {
    fun execute(): Boolean?
    fun undo(): Boolean
    fun resume(): Boolean?

    interface Behavior {
        fun executeAction(): Boolean?
        fun undoAction(): Boolean
        fun resumeAction(): Boolean? = executeAction()
        fun cleanupAction() {}
    }
}

