/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

import visitor.CommandVisitor

// Understands some action that can be (potentially) reversed
// Command patten [GoF] API definitions
interface Undoable<R> {
    // Standard do and undo, along with possibility of suspension
    fun execute(): Boolean?  // true = success; false = failure; null = suspended
    fun undo(): Boolean  // true = success; false = failure
    fun resume(r: R? = null): Boolean?  // true = success; false = failure; null = suspend_again

    // Understand status of this overall Command
    fun status(): Status

    // Visitor pattern [GoF] support to scan Command hierarchy
    fun accept(visitor: CommandVisitor<R>)

    // Facilitate additional Behaviors added to a Command
    fun inject(behavior: Behavior<R>) {}   // Ignore by default

    val identifier: Any  // Used for debugging

    // Understands the subset of Commands to be executed
    // Command pattern [GoF] composite behahaviors
    interface Composite<R>: Undoable<R> {
        fun add(step: Undoable<R>): Boolean
        fun add(index: Int, step: Undoable<R>)
        fun remove(step: Undoable<R>): Boolean
    }

    // Understands what should happen when a Command is invoked
    // Separation of Concerns: Rather than subclassing Commands, Commands delegate to Behaviors
    interface Behavior<R> {
        fun executeAction(): Boolean?
        fun undoAction(): Boolean
        fun resumeAction(r: R? = null): Boolean? = executeAction()
        fun accept(visitor: CommandVisitor<R>) {}   // Visitor pattern [GoF] support

        // Additional API invoked at Command completion, whether execute() or undo()
        fun cleanupAction() {}
    }

    // Understands to do nothing on invocation from Command
    // Null Object pattern
    class NoBehavior<R> : Behavior<R> {
        override fun executeAction() = true
        override fun undoAction() = true
    }

    // Understands tracking activity of a particular Behavior
    // Marker Interface to allow explicit tagging
    interface Trace<R>: Behavior<R>

    // Understands an external representation of Command progress
    enum class Status {
        Ready, Pending, Complete, Failure
    }
}

