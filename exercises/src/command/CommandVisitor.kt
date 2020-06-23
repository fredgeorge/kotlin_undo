/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

interface CommandVisitor {
    fun preVisit(command: Undoable, behavior: Undoable.Behavior? = null) {}
    fun visit(behavior: Undoable.Behavior) {}
    fun postVisit(command: Undoable, behavior: Undoable.Behavior? = null) {}
}