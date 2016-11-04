package okkero.util

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.util.*

/**
 * @param getBroadcastSequence a sequence of all clients ordinarily eligible to receive a broadcast command
 * @param <S> the sender type
 */
class CommandNexus<S>(
        private val sendMessage: S.(String) -> Unit,
        private val getBroadcastSequence: () -> Sequence<S>,
        private val gson: Gson = Gson()
) {

    private val nameToClass = HashMap<String, Class<out Command>>()
    private val classToHandler = HashMap<Class<out Command>, (S, Nothing) -> Unit>()

    /**
     * Register a command handler to the nexus, for handling a specific type of command.
     *
     * @param commandName  the name of the command to handle
     * @param commandClass the class of the command to handle
     * @param handler      the handler responsible for handling the command
     * @param <T>          the type of command to handle
     */
    fun <T : Command> handleCommand(commandName: String, commandClass: Class<T>, handler: (S, T) -> Unit) {
        nameToClass.put(commandName, commandClass)
        classToHandler.put(commandClass, handler)
    }

    /**
     * Register a command handler to the nexus, for handling a specific type of command.
     *
     * @param commandName  the name of the command to handle
     * @param handler      the handler responsible for handling the command
     * @param <T>          the type of command to handle
     */
    inline fun <reified T : Command> handleCommand(commandName: String, noinline handler: (S, T) -> Unit) {
        handleCommand(commandName, T::class.java, handler)
    }

    /**
     * Handle a command sent from a session. This will delegate the command handling to registered command handlers.
     *
     * @param sender the sender of the command
     * @param json   the command that was sent, in JSON format
     */
    fun onCommand(sender: S, json: String) {
        val command = parseCommand(json)
        val handler = getCommandHandler(command.javaClass)

        handler(sender, command)
    }

    /**
     * Parse a command from JSON into a command object.
     * <p>
     * The JSON is required to have a property named "commandname" with the name of the command as value.
     *
     * @return the parsed command object
     */
    fun parseCommand(json: String): Command {
        val parser = JsonParser()
        val elem = parser.parse(json)
        val commandName = elem.asJsonObject.get("commandname").asString

        return gson.fromJson(elem, getCommandType(commandName))
    }

    /**
     * Get the class of the command type with the given name.
     *
     * @param commandName name of the command type
     * @return the class associated with the given name
     */
    private fun getCommandType(commandName: String): Class<out Command> {
        return nameToClass[commandName] ?: Command::class.java
    }

    /**
     * Get the handler responsible for handling commands of the specified name.
     *
     * @param commandName the name of the command to be handled
     * @return the CommandHandler object responsible for handling commands with the name
     */
    fun getCommandHandler(commandName: String): (S, Nothing) -> Unit {
        val commandClass = getCommandType(commandName)
        return getCommandHandler(commandClass)
    }

    /**
     * Get the handler responsible for handling commands of the specified class.
     *
     * @param commandClass the class of the command to be handled
     * @param <T>          the type of command to be handled
     * @return the CommandHandler object responsible for handling commands of the type
     */
    fun <T : Command> getCommandHandler(commandClass: Class<T>): (S, T) -> Unit {
        return classToHandler[commandClass] as (S, T) -> Unit
    }

    /**
     * Get the handler responsible for handling commands of the specified class.
     *
     * @param <T>          the type of command to be handled
     * @return the CommandHandler object responsible for handling commands of the type
     */
    inline fun <reified T : Command> getCommandHandler(): (S, T) -> Unit {
        return getCommandHandler(T::class.java)
    }

    /**
     * Sends a command to a specified client.
     *
     * @param recipient the receiving client
     * @param command   the command to send
     * @throws IOException
     */
    @Throws(IOException::class)
    fun sendCommand(command: Command, recipient: S) {
        recipient.sendMessage(convertToJSON(command))
    }

    /**
     * Broadcasts a command to multiple clients, depending on a given filter.
     *
     * @param command   the command to send
     * @param predicate the predicate a client must match in order to receive the command
     * @throws IOException
     */
    @Throws(IOException::class)
    fun broadcastCommand(command: Command, predicate: (S) -> Boolean = { true }) {
        getBroadcastSequence().filter(predicate).forEach({ sendCommand(command, it) })
    }

    /**
     * Converts a command into a JSON formatted string.
     *
     * @param command the command to convert
     * @return a JSON formatted string
     */
    fun convertToJSON(command: Command): String {
        return gson.toJson(command)
    }

}

abstract class Command(name: String) {

    @SerializedName("commandname")
    val commandName = name

}