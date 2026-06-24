---
title: Intro
slug: index
description: Intro
---

Documentation about Battle Dawn.

![Gameplay](../../assets/battledawn.png)

### Scratch Pad

client.swf -> entry point of SWF if running with flash player
loader-app.xml launches client.swf (changed from loader.swf)
clientLoader.exe -> appears to be the normal, default launcher
it is used by the BattleDawn shortcut initialized from the game installer

bdBattleCalculator.swf -> battle simulation require server side response

bdClient.exe -> ??? very similar to clientLoader

running the game directly via client.swf will cause black screen
however, running the same client.swf via loader-app.xml (with ADL)
success with hourglass and the game trying to connect to the server.

that means hourglass shows that the game is trying to connect to the server

- bdclient.exe directly show hourglass
- clientLoader.exe shows loading bar with 1274 KB
  allegedly this 1274KB is client.swf

inference:

- bdclient.exe and clientLoader.exe are not real flash app, they are actually "exe" although with flash player projector icon.
  - they are probably a real SWF BUT packaged together with a standalone flash player projector
  - a flash player 32 sa size is around 15 mb, and both exe are at similar sizes
  - proof is that there exist adobe flash library inside the exe if you open in notepad and scroll down
  - YES! bdclient.exe and clientLoader.exe is actually client.swf packaged together with flash player app then baked into exe, to make player load the game easily, so they can install the game and run it directly.
    - another proof is that battledawn shortcut maps to clientLoader.exe

the difference between bdclient.exe and clientLoader.exe

- they have about 1mb difference
- they have the same adobe flash projector
- the 1 mb is probably the client.swf added into bdclient.exe
- this explains why clientLoader.exe has loading bar with 1274 KB load before going into hourglass and bdclient directly show hourglass directly

PROOF: delete bdclient.exe and clientLoader.exe
in fact you can run the game without them
this mean they are safe to delete.
however, we will keep them for simplicity in running the game for those that don't have flash player
we will delete bdclient.exe as it is the larger version
we also rename clientLoader.exe to BattleDawn.exe to make it clear

we rename the loader-app.xml to loader.xml

LaunchClient.bat is shortcut to run client.swf via adl with script : "adl loader.xml ."

we delete unnecessary construct like unins000.dat/exe

there is amount 5k duplicate files and up to 85 MB save on each resources directory
we do not delete because it will be easier to serve the game if we do not delete
one of the notable duplicates are the map files. althohugh we can create mapping of duplciate map grid, then redirecting those files, it will waste time

### Redirection

1. domainUrl.php is changed to localhost

```
domain=127.0.0.1:8080&forumReg=http://127.0.0.1:8080/forum?mode=register&theme=earthMarsFantasy&packageURL=http://127.0.0.1:8080/packages&forumURL=http://127.0.0.1:8080/forum
```

2. `ClientManager.as` modified so that `AcctService` when run remotely (i.e., from website) it would load the domain + some port number which is hardcoded to `8080`.
3. URL for music and tutorial audio is redirected to `music/Intro.mp3` and `/tutorialAudio/{world}/{audioname}` respectively.

### BattleDawn Networking

Battledawn uses http api for network communication (not raw socket)

the api rely on adobe's NetConnection object whihc uses the AMF3 format by default

Example:

```
as ascii string
�.net.battlegate.secure.AcctServices.getUserData�/1�getUserData

as hex
00 03 00 00 00 01

00 2E 6E 65 74 2E 62 61 74 74 6C 65 67 61
74 65 2E 73 65 63 75 72 65 2E 41 63 63 74 53 65 72 76 69 63
65 73 2E 67 65 74 55 73 65 72 44 61 74 61 00 02 2F 31

00 00 00 13 0A

00 00 00 01 02 00 0B 67 65 74 55 73 65 72 44 61 74 61
```

Specifically, the payload is amf3 but wrapped in amf0

```
00 03 00 00 00 01

00 03 — AMF Version: AMF3 is used in the body.
00 00 — Header Count: 0, no headers.
00 01 — Message Body Count: 1, only 1 message.

---

00 2E 6E 65 74 ... 2F 31

00 2E — Target URI Length: 46 bytes (0x2E in decimal).
6E 65 74 ... 61 74 61 — Target URI Value: Text string net.battlegate.secure.AcctServices.getUserData.
00 02 — Response/Response Tracking ID Length: 2 bytes.
2F 31 — Response Tracking Value: Text string /1.

---

00 00 00 13 0A 00 00 00 01 02 00 0B 67 65 74 55 73 65 72 44 61 74 61

00 00 00 13 — Body Length: 19 bytes (0x13 in decimal), payload is 19 bytes.

body:
0A — AMF0 Type Marker: Denotes an array.
00 00 00 01 — Element count: only 1 array element.
02 — AMF0 Type Marker: Denotes a string.
00 0B — String Length: Indicates the string length is 11 bytes (0x0B in decimal).
67 65 74 55 73 65 72 44 61 74 61 — String Payload: Text string getUserData.

AMF Packet
 ├─ version
 ├─ headers
 └─ messages
      ├─ target URI
      ├─ response URI
      └─ body
            AMF0 marker 0x0A (strict array)
                 [ arg0, arg1, ... ]
```

notable class:

- RemoteManager
- RemotingManager
- RemotingService
- RemotingCall
- RemotingConnection
- NetConnection (adobe)
- events classes

The game creates various service (sort of the manager of each domain)
the service is associated with an ID for identification and endpoint
service is registered with listener

the endpoint is HTTPS and is used to create RemotingConnection

RemotingConnection is a wrapper over adobe's NetConnection

each network communication creates a RemotingCall which sends message to server
through the NetConnection. the remoting call receives a success/failure callback.

the network call has URI like
net.battlegate.secure.AcctServices.getUserData
and "method" like getUserData
the identifier like /1 /2 /3 reprersent the request id

The progress of request message is represented as CallEvent. THis is dispatched in client side as event, such as REQUEST_SENT, RESULT, FAULT, etc. The networking is also event-driven, dispatching event class containing result or notification of network request/response.

The adobe NetConnection itself takes a "command" of method for the server.
it also includes responsder (client callback to handle response) and variable amount of arguments

ResultEvent is generated when success result from server is received, while FaultEvent for failure result.

ResultEvent contains object from the server. Once result event is created, the result callback from client is called.

response from server include

- param1 = object payload of data
- param2 = some array(?) where first element is the request's method

#### Networking Flow

```as3
RemoteManager.getUserData()

this.remotingManager.call(this.AcctServicesID,"getUserData",["getUserData"],this.onGeneralResult,this.onFault,true);
```

- param1 serviceId: String = AcctServicesID ("AcctServices")
- param2 method: String = "getUserData"
- param3 args: Array = ["getUserData"]
- param4 resultCallback: Function = onGeneralResult
- param5 faultCallback: Function = onFault
- param6 returnArgs: Boolean = true (whether to return the args to the caller on both result/fault callbacks)

```as3
RemotingManager.call(6 params) -> RemotingService.call(5 params; -serviceId)

RemotingService.call() creates CallEvent, creates RemotingCall and call RemotingCall.execute()
```

- param1 remoteServiceClass: RemotingService
- param2 method: String = "getUserData"
- param3 result: Function = onGeneralResult
- param4 fault: Function = onFault
- param5 args: Array = ["getUserData"]
- param6 returnArgs: Boolean = true (default=false)
- param7 callTimeout: Int = 30000 (default)
- param7 maxRetries: Int = 3 (default)

RemotingCall.execute() creates adobe NetConnection and invoke the call method
this waits until response is received from server and result or fault callback will be called

In this case, the onGeneralResult on RemoteManager is called

- param1 resultEvent: ResultEvent
- param2 args: Array

in the onGeneralResult, the first element of param2 contains the earlier args ["getUserData"]. this is popped and is used to removeFromPendingList from cursorM.

It proceed to call the generalResultFunctions passing in ResultEvent, first element of args, and rest of args

```as3
_loc3_ = new ResultEvent(param1,false,true);
if (this.returnArgs) {
  this.result(_loc3_,this.args);
}
else {
  this.result(_loc3_);
}

private function onGeneralResult(param1:ResultEvent, param2:Array = null) : void

this.generalResultFunctions(param1.result,param2.pop(),param2);
```

Inside generalResultFunctions is the generic handler of the game.

if param1["success"] == true || param2 == "getUserData"

this param1 is required to be sent by server. this is an object an i think must be AMF format.

failure at network level is represented as "fault"
failure/success at application level is represented with param1["success"]

for example

param1["success"] = true on "getUserData" lets the client call:

```as3
this.dataM.gotUserData(param1["user_id"],param1["ROLES"],param1["display_name"],param1["avatar_data"]);
this.getPreLoginData();
```

while param1["success"] = false on "buildSquadUnits" contains sub result message param1["result"] which denotes the reason message such as "ERR_TICK_OUT_OF_SYNC".

#### TLDR Flow

```
RemoteManager.getUserData()

RemotingManager.call(serviceId, method, args, resultCallback, faultCallback, returnArgs)

...find RemotingService with serviceId
RemotingService.call(method, args, resultCallback, faultCallback, returnArgs)

...creates RemotingCall
RemotingCall.execute(remotingServiceClass, method, resultCallback, faultCallback, args, returnArgs)

...creates NetConnection
...has internal onResult and onFault
NetConnection.call(null, Responder(onResult, onFault), args)

...when received server response
...onResult called
...ResultEvent created
resultCallback(objectResponseFromServer, args)

...bubbles up to RemoteManager
...callback of onGeneralResult
...call generalResultFunctions(objectResponseFromServer, args.pop(), args)

...generalResultFunctions handles each case of success/failure
and handle based on args, then inspect the object sent from server
```
