/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

import command.Undoable.Status.*
import decorator.ActionTracer
import visitor.CommandPrettyPrint
import visitor.CommandVisitor

// Understands the sequential execution of a series of sub-Commands
// Implements the composite component of the Composite pattern [GoF]
class SerialCompositeCommand<R> private constructor(
        private val steps: MutableList<Undoable<R>>,
        private var behavior: Undoable.Behavior<R>,
        override val identifier: Any = "<unidentified SerialCompositeCommand>",
        private var currentStep: Undoable<R> = steps.first()
) : Undoable.Composite<R> {

    private var nextSteps: Map<Undoable<R>, Undoable<R>> = emptyMap()
    private var previousSteps: Map<Undoable<R>, Undoable<R>> = emptyMap()

    init {
        generateLinkages()
    }

    constructor(
            vararg steps: Undoable<R>,
            behavior: Undoable.Behavior<R> = Undoable.NoBehavior(), // Optional, and probably infrequent
            identifier: Any = "<unidentified SerialCompositeCommand>"
    ) : this(
            steps.toMutableList().also { if (it.isEmpty()) it.add(NullStep()) }, // Avoid isEmpty() checks later
            behavior,
            identifier
    )

    override fun execute(): Boolean? {
        behavior.executeAction().also { result -> if (result != true) return result } // Abort early
        return executeCurrentStep().also { if (it != null) behavior.cleanupAction() }
    }

    override fun undo(): Boolean {
        behavior.undoAction().also { result -> if (!result) return false }
        currentStep = steps.lastOrNull() ?: NullStep()
        return undoCurrentStep().also { behavior.cleanupAction() }
    }

    override fun resume(r: R?): Boolean? {
        currentStep.resume(r).also { resumeResult ->
            return when (resumeResult) {
                true -> if (isLastStep()) true else executeCurrentStep()
                false -> rollback()
                else -> null  // it's suspended again
            }.also { behavior.cleanupAction() }
        }
    }

    override fun status(): Undoable.Status {
        steps.map { it.status() }.let { statuses ->
            if (statuses.all { it == Ready }) return Ready
            if (statuses.any { it == Failure}) return Failure
            if (statuses.any { it == Pending}) return Pending
            return Complete
        }
    }

    override fun accept(visitor: CommandVisitor<R>) {
        val realSteps = steps.filterNot { it is NullStep }
        val realCurrent = if (currentStep is NullStep) null else currentStep
        visitor.preVisit(this, realSteps, realCurrent, behavior, status())
        behavior.accept(visitor)
        steps.filterNot { it is NullStep }.forEach { it.accept(visitor) }
        visitor.postVisit(this, realSteps, realCurrent, behavior, status())
    }

    override fun inject(behavior: Undoable.Behavior<R>) {
        this.behavior = behavior
    }

    override fun add(step: Undoable<R>) = steps.add(step).also {
        generateLinkages()
    }

    // Inject sub-Commands in midst of other steps; no change to current step
    override fun add(index: Int, step: Undoable<R>) = steps.add(index, step).also {
        generateLinkages()
    }

    override fun remove(step: Undoable<R>): Boolean {
        adjustCurrentStep(step)
        return steps.remove(step).also { generateLinkages() }
    }

    private fun adjustCurrentStep(step: Undoable<R>) {
        when {
            currentStep != step -> return
            currentStep.status() == Pending ->
                throw IllegalStateException("Cannot remove a suspended Command; attempt resume() or undo() first.")
            steps.size == 1 -> {    // Removing only step
                currentStep = NullStep()
                steps.add(currentStep)
            }
            nextSteps[currentStep] == null -> currentStep = previousSteps[currentStep]!!  // Already handle only step
            else -> currentStep = nextSteps[currentStep]!!
        }
    }

    // Create convenience structures to allow next() and prior() in Command steps
    private fun generateLinkages() {
        nextSteps = this.steps.zipWithNext().toMap()  // pseudo linked list for execution
        previousSteps = this.steps.reversed().zipWithNext().toMap() // pseudo linked list for undo
    }

    override fun toString() = CommandPrettyPrint(this).result()

    // Turn on Command hierarchy tracing
    fun trace() = ActionTracer(this)

    // Recursive execution
    private fun executeCurrentStep(): Boolean? {
        currentStep.execute().also { executeResult ->
            return when (executeResult) {
                true -> if (isLastStep()) true else executeCurrentStep() // Keep going if it worked
                false -> rollback()  // Whoops! Try to reset everything
                else -> null  // it's suspended at the currentStep
            }
        }
    }

    private fun isLastStep(): Boolean {
        nextSteps[currentStep].let { nextStep ->
            if (nextStep == null) return true
            currentStep = nextStep
            return false
        }
    }

    private fun rollback(): Boolean {
        if (behavior.undoAction() && !isFirstStep()) undoCurrentStep()
        return false
    }

    private fun isFirstStep(): Boolean {
        previousSteps[currentStep].let { previousStep ->
            if (previousStep == null) return true
            currentStep = previousStep
            return false
        }
    }

    // Recursive undo
    private fun undoCurrentStep(): Boolean {
        currentStep.undo().also { undoResult ->
            if (!undoResult) return false  // Stop recursive undo on first failure
            return if (!isFirstStep()) undoCurrentStep() else true
        }
    }

    // Serves as a step in any empty Composite Command to avoid constant isEmpty() checks
    // Null Object pattern
    private class NullStep<R> : Undoable<R> {
        override fun execute() = true
        override fun undo() = true
        override fun resume(r: R?) = true
        override fun status() = Complete
        override fun accept(visitor: CommandVisitor<R>) {} // Ignore making it invisible to visitors
        override fun toString() = identifier
        override val identifier = "<no steps>"
    }
}