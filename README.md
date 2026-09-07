# LazySwitcher

Switch Minecraft accounts **in-game** without restarting, and get reconnected to
the server you were on as the new account.

- **Fabric**, client-side only, **Minecraft 1.21.11**
- Works on Windows, Linux and macOS
- Nothing to configure — drop the jar in your `mods` folder and go

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for 1.21.11 and
   [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download `lazyswitcher-<version>.jar` from
   [Releases](../../releases) and put it in your `.minecraft/mods` folder
   (or your Prism/MultiMC instance's `mods` folder).
3. Launch. You'll see a **LazySwitcher** button on the title screen and in the
   pause menu.

## Using it

Open LazySwitcher (title screen or pause menu), pick an account, hit **LazySwitcher**.

- If you're **on a server**, it signs in as the new account and **reconnects you
  to that same server** automatically.
- If you're on a menu, it just swaps the account.
- **↺ &lt;name&gt;** reverts to whatever account the game launched with.

### Adding accounts

**Add Account** supports:

| Type | What you need |
|------|---------------|
| **Microsoft** | Nothing. Click confirm, a code + `microsoft.com/link` appears, sign in in your browser. Leave the Client ID box blank to use the built-in default. |
| **Offline / Cracked** | A username. |
| **Alt service** | A [TheAltening](https://thealtening.com) or [EasyMC](https://easymc.io) token. |

### Prism / MultiMC / PolyMC users

Accounts already signed into your launcher show up automatically as `[Prism] name`
— no need to add them again. LazySwitcher refreshes their tokens when needed.
If a Prism account ever fails to switch, launch it once in the launcher and
reopen the menu.

## Notes

- Account data you add lives in `config/accountswitcher/` next to your instance.
  Nothing is sent anywhere except Microsoft/Mojang's own auth servers.
- On servers that enforce signed chat, chat may stay disabled for the switched
  account until you restart the game. Joining and playing still work.
- Behind a VPN with an IPv6 kill-switch? Add `-Djava.net.preferIPv4Stack=true`
  to your Java arguments or Mojang auth can hang.

## Build from source

```
./gradlew build
# jar in build/libs/
```

Requires JDK 21.

## License

MIT
