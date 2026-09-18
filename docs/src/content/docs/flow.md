---
title: Flow
slug: flow
description: Flow
---

This list the flow of message from game SWF startup up to entering the game.

1. `net.battlegate.secure.AcctServices.getUserData`
2. `com.battledawn.insecure.BDGlobalsIServices.getPreLoginData`
   -> isLoggedIn=true: `com.battledawn.insecure.BDGlobalsIServices.getMyWorlds`
   -> isLoggedIn=false continues to 3
3. `com.battledawn.secure.BDUserSServices.createTemporaryAccount`

However, `createTemporaryAccount` does not continue to anything else. We can force a progress by modifying the client to load `loginGfx.swf` to force the client to proceed to the world selection screen.

After that, the player should select and enter a particular world.

4. `net.battlegate.secure.AcctServices.getUserData` (repeat)
5. `com.battledawn.insecure.BDRulerIServices.userHasRuler`
6. `com.battledawn.insecure.BDGlobalsIServices.getI18NTableNew`
7. `com.battledawn.insecure.BDGlobalsIServices.getAllTablesNoColony`
8. `com.battledawn.insecure.BDGameIServices.getLocalSettingsTable`
9. `com.battledawn.insecure.BDMapIServices.getMapSettings`
10. `com.battledawn.insecure.BDRulerIServices.getAllRulers`
11. `com.battledawn.insecure.BDExternalAccountIServices.getExternalAccounts`
12. `com.battledawn.insecure.BDAllianceIServices.getAllAlliances`
13. `com.battledawn.insecure.BDMapIServices.getColonies`
