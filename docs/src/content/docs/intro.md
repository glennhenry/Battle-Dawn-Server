---
title: Intro
slug: index
description: Intro
---

Documentation about Battle Dawn.

![Gameplay](../../assets/battledawn.png)

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


