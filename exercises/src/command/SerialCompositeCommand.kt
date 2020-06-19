package command

class SerialCompositeCommand(vararg steps: Undoable, private val behavior: Undoable.Behavior): Undoable {
    private val steps = steps.toList()
    private val nextSteps = this.steps.zipWithNext().toMap()  // pseudo linked list for execution
    private val previousSteps = this.steps.reversed().zipWithNext().toMap() // pseudo linked list for undo
    private var currentStep = this.steps.firstOrNull()

    override fun execute(): Boolean? {
        behavior.executeAction().also { result ->
            if (result != true) return result
        }
        return executeCurrentStep()
    }

    private fun executeCurrentStep(): Boolean? {
        if (currentStep == null) {
            behavior.cleanupAction()
            return true
        }
        currentStep?.also { step ->
            step.execute().also { executeResult ->
                if (executeResult == null) return null
                if (!executeResult) {
                    behavior.undoAction().also { undoResult ->
                        if (!undoResult) {
                            behavior.cleanupAction()
                            return undoResult
                        }
                    }
                    currentStep = previousSteps[step]
                    undoCurrentStep()
                    return false
                }
                currentStep = nextSteps[step]
                return executeCurrentStep()
            }
        }
        throw IllegalStateException("Unexpected flow issue in recursion")
    }

    override fun undo(): Boolean {
        behavior.undoAction().also { result ->
            if (!result) return result
        }
        currentStep = steps.lastOrNull()
        return undoCurrentStep()
    }

    private fun undoCurrentStep(): Boolean {
        if (currentStep == null) {
            behavior.cleanupAction()
            return true
        }
        return currentStep!!.let { step ->
            step.undo().also { result ->
                if (!result) {
                    behavior.cleanupAction()
                    return false
                }
                currentStep = previousSteps[step]
                undoCurrentStep()
            }
        }
    }

    override fun resume() = executeCurrentStep()
}