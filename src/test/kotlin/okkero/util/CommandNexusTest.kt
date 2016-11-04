package okkero.util

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class CommandNexusTest {

    lateinit var sender: MockSender
    lateinit var nexus: CommandNexus<MockSender>

    @Before
    fun setup() {
        sender = MockSender()
        nexus = CommandNexus(MockSender::send, { emptySequence() })
    }

    @Test
    fun `receives correct Command type with correct data given JSON`() {
        val json = readJSONFromResource("mockcommand.json")

        var message: String? = null
        nexus.handleCommand("mock", { sender, command: MockCommand -> message = command.message })
        nexus.onCommand(sender, json)

        assertEquals("mock123", message)
    }

    @Test
    fun `sends correct JSON data given Command instance`() {
        assertNull(sender.lastSentMessage)

        val cmd = MockCommand()
        cmd.message = "abc"
        nexus.sendCommand(cmd, sender)

        val obj = JsonParser().parse(sender.lastSentMessage).asJsonObject
        assertEquals("mock", obj["commandname"].asString)
        assertEquals(cmd.message, obj["message"].asString)
    }

    private fun readJSONFromResource(resource: String): String {
        val path = Paths.get(javaClass.getResource(resource).toURI())
        return String(Files.readAllBytes(path))
    }

}

class MockCommand : Command("mock") {

    var message: String? = null

}

class MockSender {

    var lastSentMessage: String? = null
        private set

    fun send(message: String) {
        lastSentMessage = message
    }

}