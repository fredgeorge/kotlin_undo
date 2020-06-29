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

class SerialCompositeCommand<R> private constructor(
        private val steps: MutableList<Undoable<R>>,
        private var behavior: Undoable.Behavior<R>,
        override val identifier: Any = "<unidentified SerialCompositeCommand>",
        private var currentStep: Undoable<R> = steps.firstOrNull() ?: NullStep<R>()
) : Undoable.Composite<R> {

    private var nextSteps: Map<Undoable<R>, Undoable<R>> = emptyMap()
    private var previousSteps: Map<Undoable<R>, Undoable<R>> = emptyMap()
    private val nullStep = NullStep<R>()

    init {
        if (steps.isEmpty()) steps.add(nullStep)
        currentStep = this.steps.firstOrNull() ?: nullStep
        generateLinkages()
    }

    constructor(
            vararg steps: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            identifier: Any = "<unidentified SerialCompositeCommand>"
    ) : this(
            steps.toMutableList(),
            behavior,
            identifier
    )

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

    override fun status(): Undoable.Status {
        steps.map { it.status() }.let { statuses ->
            if (statuses.all { it == Ready }) return Ready
            if (statuses.any { it == Failure}) return Failure
            if (statuses.any { it == Pending}) return Pending
            return Complete
        }
    }

    override fun accept(visitor: CommandVisitor<R>) {
        visitor.preVisit(this, steps, currentStep, behavior, status())
        behavior.accept(visitor)
        steps.filterNot { it is NullStep }.forEach { it.accept(visitor) }
        visitor.postVisit(this, steps, currentStep, behavior, status())
    }

    override fun inject(behavior: Undoable.Behavior<R>) {
        this.behavior = behavior
    }

    override fun add(step: Undoable<R>) = steps.add(step).also {
        generateLinkages()
    }

    override fun add(index: Int, step: Undoable<R>) = steps.add(index, step).also {
        generateLinkages()
    }

    override fun remove(step: Undoable<R>) = steps.remove(step).also {
        generateLinkages()
    }

    private fun generateLinkages() {
        nextSteps = this.steps.zipWithNext().toMap()  // pseudo linked list for execution
        previousSteps = this.steps.reversed().zipWithNext().toMap() // pseudo linked list for undo
    }

    override fun toString() = CommandPrettyPrint(this).result()

    fun trace() = ActionTracer(this)

    // Recursive execution
    private fun executeCurrentStep(): Boolean? {
        currentStep.execute().also { executeResult ->
            return when (executeResult) {
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

    private class NullStep<R> : Undoable<R> {
        override fun execute() = true
        override fun undo() = true
        override fun resume(r: R?) = true
        override fun status() = Complete
        override fun accept(visitor: CommandVisitor<R>) {} // Ignore
        override val identifier = "<no steps>"
    }
}