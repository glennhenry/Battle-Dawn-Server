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

00 00 00 13 0A

00 00 00 13 — Body Length: 19 bytes (0x13 in decimal), payload is 19 bytes.
0A — AMF0 Type Marker (AvmPlusObject): Switch from AMF0 to AMF3 rules.

---

00 00 00 01 02 00 0B 67 65 74 55 73 65 72 44 61 74 61

00 — AMF3 Array Header.
00 00 01 — Element Count: only 1 element.
02 — AMF3 Type Marker: This is a String marker.
00 0B — String Length: Indicates the string length is 11 bytes (0x0B in decimal).
67 65 74 55 73 65 72 44 61 74 61 — String Payload: Text string getUserData.
```

notable class:
- RemoteManager
- RemotingManager
- RemotingService
- RemotingCall
- RemotingConnection
- NetConnection (adobe)
- CallEvent

The game creates various service (sort of the manager of each domain)
the service is associated with an ID for identification and endpoint
service is registered with listener

the endpoint is HTTPS and is used to create RemotingConnection

RemotingConnection is a wrapper over adobe's NetConnection

each network communication generate a RemotingCall which sends message to server
through the NetConnection. the network call has URI like
net.battlegate.secure.AcctServices.getUserData
and "method" like getUserData
the identifier  like /1 /2 /3 reprersent the request id

This results in CallEvent which eventually reach the RemotingManager
and notify the service's listener

response from server include

param1 = object payload of data
param2 = some array(?) where first element is the request's method
