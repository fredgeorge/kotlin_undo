/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

import visitor.CommandVisitor

interface Undoable<R> {
    fun execute(): Boolean?
    fun undo(): Boolean
    fun resume(r: R? = null): Boolean?
    fun accept(visitor: CommandVisitor<R>)
    fun inject(behavior: Behavior<R>) {}   // Ignore by default
    val identifier: Any  // Used for debugging

    interface Composite<R>: Undoable<R> {
        fun add(step: Undoable<R>): Boolean
        fun add(index: Int, step: Undoable<R>)
        fun remove(step: Undoable<R>): Boolean
    }

    interface Behavior<R> {
        fun executeAction(): Boolean?
        fun undoAction(): Boolean
        fun resumeAction(r: R? = null): Boolean? = executeAction()
        fun cleanupAction() {}
        fun accept(visitor: CommandVisitor<R>) {}
    }

    interface Trace<R>: Behavior<R>

    enum class Status {
        Ready, Pending, Complete, Failure
    }
}

