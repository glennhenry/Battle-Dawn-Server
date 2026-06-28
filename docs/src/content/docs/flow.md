---
title: Flow
slug: flow
description: Flow
---

1. `net.battlegate.secure.AcctServices.getUserData`
2. `com.battledawn.insecure.BDGlobalsIServices.getPreLoginData`
    -> isLoggedIn=true: `com.battledawn.insecure.BDGlobalsIServices.getMyWorlds`
    -> isLoggedIn=false continues to 3
3. `com.battledawn.secure.BDUserSServices.createTemporaryAccount`

However, createTemporaryAccount does not connect anywhere else.
You can force createTemporaryAccount to world selection screen
if you send "authenticationSuccess" to screenLogin.refreshLoginStatus
while also calling getLoginGfx manually.
