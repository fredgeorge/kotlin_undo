/*
 * Copyright (c) 2020 by Fred George
 * MIT License - see LICENSE file
 * @author Fred George  fredgeorge@acm.org
 */

package command
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import visitor.CommandVisitor

class JsonPersistence<R>(command: Undoable<R>) : CommandVisitor<R> {
    private val mapper = ObjectMapper()
    private val commandObjectNodes: MutableList<ObjectNode> = mutableListOf()
    private val arrayNodes: MutableList<ArrayNode> = mutableListOf(mapper.createArrayNode())
    private lateinit var behaviorNodes: ArrayNode
    private var root: ObjectNode = mapper.createObjectNode()

    init {
        command.accept(this)
    }

    fun result() = root

    override fun toString() = ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writeValueAsString(result())

    override fun preVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>?,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        mapper.createObjectNode().also { currentComposite ->
            currentComposite.put("identifier", command.identifier.toString())
            currentComposite.put("kclass", command.javaClass.simpleName)
            currentComposite.put("status", command.status().name)
            behaviorNodes = mapper.createArrayNode()
            arrayNodes.first().add(currentComposite)
            commandObjectNodes.add(0, currentComposite)
            arrayNodes.add(0, mapper.createArrayNode())
        }
    }

    override fun postVisit(
            command: Undoable.Composite<R>,
            steps: List<Undoable<R>>,
            currentStep: Undoable<R>?,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        commandObjectNodes.first().also { currentComposite ->
            currentComposite.set("behaviors", behaviorNodes)
            currentComposite.set("steps", arrayNodes.removeAt(0))
            root = currentComposite  // In case we are done
        }
        commandObjectNodes.removeAt(0)
    }

    override fun preVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        mapper.createObjectNode().also { currentLeaf ->
            currentLeaf.put("identifier", command.identifier.toString())
            currentLeaf.put("status", status.name)
            behaviorNodes = mapper.createArrayNode()
            arrayNodes.first().add(currentLeaf)
            commandObjectNodes.add(0, currentLeaf)
        }
    }

    override fun postVisit(
            command: Undoable<R>,
            behavior: Undoable.Behavior<R>,
            status: Undoable.Status
    ) {
        commandObjectNodes.first().set("behaviors", behaviorNodes)
        commandObjectNodes.removeAt(0)
    }

    override fun visit(behavior: Undoable.Behavior<R>) {
        mapper.createObjectNode().also { currentBehavior ->
            currentBehavior.put("kclass", behavior.javaClass.simpleName)
            behaviorNodes.add(currentBehavior)
        }
    }
}