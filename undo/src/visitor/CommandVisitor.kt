/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package visitor

import command.Undoable

// Understands walking a Command hierarchy
// Interface to Visitor pattern [GoF] for Commands
// Passes many elements as suggested in Observer pattern [GoF] to support Command encapsulation
interface CommandVisitor<R> {
    fun preVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>?,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {}
    fun preVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {}
    fun visit(behavior: Undoable.Behavior<R>) {}
    fun postVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {}
    fun postVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>?,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {}
}