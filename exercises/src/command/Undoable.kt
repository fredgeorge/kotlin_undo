/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

interface Undoable {
    fun execute(): Boolean?
    fun undo(): Boolean
    fun resume(): Boolean?
    fun accept(visitor: CommandVisitor)
    val identifier: Any  // Used for tracing

    interface Behavior {
        fun executeAction(): Boolean?
        fun undoAction(): Boolean
        fun resumeAction(): Boolean? = executeAction()
        fun cleanupAction() {}
        fun accept(visitor: CommandVisitor) {}
    }
}

