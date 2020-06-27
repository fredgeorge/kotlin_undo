/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command

import decorator.ActionTracer
import visitor.CommandPrettyPrint
import visitor.CommandVisitor

class SerialCompositeCommand<R>(
        vararg steps: Undoable<R>,
        private var behavior: Undoable.Behavior<R>,
        override val identifier: Any = "<unidentified SerialCompositeCommand>"
): Undoable.Composite<R> {
    private val steps: MutableList<Undoable<R>>
    private var currentStep: Undoable<R>
    private val nextSteps: Map<Undoable<R>, Undoable<R>>
    private val previousSteps: Map<Undoable<R>, Undoable<R>>
    private val nullStep = NullStep<R>()

    init {
        this.steps = if (steps.isEmpty()) mutableListOf(nullStep) else steps.toMutableList()
        currentStep = this.steps.firstOrNull() ?: nullStep
        nextSteps = this.steps.zipWithNext().toMap()  // pseudo linked list for execution
        previousSteps = this.steps.reversed().zipWithNext().toMap() // pseudo linked list for undo
    }

    override fun execute(): Boolean? {
        behavior.executeAction().also { result -> if (result != true) return result } // Abort early
        return executeCurrentStep().also { if (it != null) behavior.cleanupAction() }
    }

    override fun undo(): Boolean {
        behavior.undoAction().also { result -> if (!result) return false }
        currentStep = steps.lastOrNull() ?: nullStep
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

    override fun accept(visitor: CommandVisitor<R>) {
        visitor.preVisit(this, steps, currentStep, behavior)
        behavior.accept(visitor)
        steps.filterNot { it is NullStep }.forEach { it.accept(visitor) }
        visitor.postVisit(this, steps, currentStep, behavior)
    }

    override fun inject(behavior: Undoable.Behavior<R>) {
        this.behavior = behavior
    }

    override fun add(step: Undoable<R>) = steps.add(step)

    override fun add(index: Int, step: Undoable<R>) = steps.add(index, step)

    override fun remove(step: Undoable<R>) = steps.remove(step)

    override fun toString() = CommandPrettyPrint(this).result()

    fun trace() = ActionTracer(this)

    // Recursive execution
    private fun executeCurrentStep(): Boolean? {
        currentStep.execute().also { executeResult ->
            return when(executeResult) {
                true -> if (isLastStep()) true else executeCurrentStep()
                false -> rollback()
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

    private class NullStep<R>: Undoable<R> {
        override fun execute() = true
        override fun undo() = true
        override fun resume(r: R?) = true
        override fun accept(visitor: CommandVisitor<R>) {} // Ignore
        override val identifier = "<no steps>"
    }
}