package command

class SerialCompositeCommand(vararg steps: Undoable, private val behavior: Undoable.Behavior): Undoable {
    private val steps: List<Undoable>
    private var currentStep: Undoable
    private val nextSteps: Map<Undoable, Undoable>
    private val previousSteps: Map<Undoable, Undoable>

    init {
        this.steps = if (steps.isEmpty()) listOf(NullStep) else steps.toList()
        currentStep = this.steps.firstOrNull() ?: NullStep
        nextSteps = this.steps.zipWithNext().toMap()  // pseudo linked list for execution
        previousSteps = this.steps.reversed().zipWithNext().toMap() // pseudo linked list for undo
    }

    override fun execute(): Boolean? {
        behavior.executeAction().also { result -> if (result != true) return result } // Abort early
        return executeCurrentStep().also { if (it != null) behavior.cleanupAction() }
    }

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

    override fun undo(): Boolean {
        behavior.undoAction().also { result -> if (!result) return false }
        currentStep = steps.lastOrNull() ?: NullStep
        return undoCurrentStep().also { behavior.cleanupAction() }
    }

    // Recursive undo
    private fun undoCurrentStep(): Boolean {
        currentStep.undo().also { undoResult ->
            if (!undoResult) return false  // Stop recursive undo on first failure
            return if (!isFirstStep()) undoCurrentStep() else true
        }
    }

    override fun resume(): Boolean? {
        currentStep.resume().also { resumeResult ->
            return when (resumeResult) {
                true -> if (isLastStep()) true else executeCurrentStep()
                false -> rollback()
                else -> null  // it's suspended again
            }.also { behavior.cleanupAction() }
        }
    }

    private object NullStep: Undoable {
        override fun execute() = true
        override fun undo() = true
        override fun resume() = true
    }
}