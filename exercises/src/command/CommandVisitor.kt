/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

interface CommandVisitor {
    fun preVisit(command: SerialCompositeCommand) {}
    fun preVisit(command: StatefulCommand) {}
    fun preVisit(command: Undoable) {}
    fun visit(behavior: Undoable.Behavior) {}
    fun postVisit(command: Undoable) {}
    fun postVisit(command: StatefulCommand) {}
    fun postVisit(command: SerialCompositeCommand) {}
}