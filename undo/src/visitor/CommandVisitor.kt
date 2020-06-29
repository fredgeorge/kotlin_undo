/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package visitor

import command.Undoable

interface CommandVisitor<R> {
    fun preVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>,
            behavior: Undoable.Behavior<R>? = null
    ) {}
    fun preVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>? = null,
            status: Undoable.Status
    ) {}
    fun visit(behavior: Undoable.Behavior<R>) {}
    fun postVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>? = null,
            status: Undoable.Status
    ) {}
    fun postVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>,
            behavior: Undoable.Behavior<R>? = null
    ) {}
}