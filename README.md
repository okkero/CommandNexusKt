# CommandNexusKt
Simple utility for serializing and deserialising commands to JSON for sending between network clients. Kotlin version.

## Setup
The CommandNexus requires a client (or server) type that the commands will be sent through. This could be a webapp client, a socket client or other.
For the sake of this guide, we will just use a plain old class:
```kotlin
class MyClient {

    fun sendMessage(message: String) {
        //Send message to the client
    }

}
```
The clients should call the nexus' onCommand method whenever they receive a command to be handled.

The CommandNexus itself can now be instantiated using this client class:
```kotlin
val nexus = CommandNexus(MyClient::sendMessage, { /*<code to retrieve a Sequence of all clients>*/ })
```

## Handling commands
To handle a command we need a class to represent the command. Say we have a registry for people. Each person has an ID to uniquely identify them, a name and an age.
```kotlin
class PersonInfoCommand : Command("person_info") { //person_info is the command name

    //val is fine; Gson alters backing fields by reflection
    //create your own default values (or use lateinit)
    val id: Int = 0
    val name: String = ""
    val age: Int = 0

}
```
Now we need to tell the nexus we want to handle all incoming commands with the name "person_info":
```kotlin
        nexus.handleCommand<PersonInfoCommand>("person_info", { sender, command -> 
            //Sender is of type MyClient
            //Command is of type PersonInfoCommand
            //We can access its getters:
            println(command.getId());
            println(command.getName());
            println(command.getAge());
        });
```
If the nexus' onCommand was called with the following JSON input:
```json
{
  "commandname": "person_info",
  "id": 42,
  "name": "John Smith",
  "age": 26
}
```
then the generated PersonInfoCommand object will have the corresponding values.

## Sending a command
To send a command, we need to define a class representing the command to send. Say we wanted to request info about a person from one of the clients by ID.
```kotlin
class RequestPersonInfoCommand(val id: Int) : Command("person_info_request") //person_info_request is the command name
```

To send this command to a client it needs to be serialized (into JSON). The CommandNexus can help us with this:
```kotlin
        val client = <some client of type MyClient>
        try {
            nexus.sendCommand(client, RequestPersonInfoCommand(2));
        } catch (e: IOException) {
            //Any IOException thrown by the client when sending the command can be handled here (or not, because this is kotlin)
            e.printStackTrace();
        }
```
This will generate the following JSON and send it to the client:
```json
{
  "commandname": "person_info_request",
  "id": 2
}
```
