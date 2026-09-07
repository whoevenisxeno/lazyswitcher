# LazySwitcher

A Fabric client mod for Minecraft. Switch accounts in game (Microsoft, offline, alt service, Prism/PolyMC/MultiMC import) from the title screen or pause menu, without restarting the game. If you switch while connected to a server, it reconnects you to that server under the new account.

## Supported Minecraft versions

- 1.20.1
- 1.20.6
- 1.21.1
- 1.21.11

Each version gets its own jar. There is no single jar that runs on every version, since Minecraft's internal APIs (session handling, connection screens, some GUI widgets) change between versions in ways that require different compiled code per version.

## Features

- Switch between Microsoft, offline, alt service, and Prism-imported accounts
- Reconnects to the current server after switching (via `SessionSwapper` and `ServerReconnector`)
- Rebuilds the auth-bound `UserApiService` and `ProfileKeys` on switch, so chat signing follows the new account where the game version supports it
- Microsoft sign in uses a self-contained OAuth device code flow, no external dependency needed
- Cross-platform Prism/PolyMC/MultiMC account discovery

## Building

Requires Java 21 to run Gradle (Java 17 is auto-provisioned for the 1.20.1 target via Gradle toolchains).

Build one version:

```
./gradlew :1.21.11:build
```

Build every declared version and collect the jars:

```
./gradlew buildAndCollect
```

Output jars land in `build/libs/<mod version>/`, one jar (plus a sources jar) per Minecraft version, for example:

```
accountswitcher-2.6.1+1.20.1.jar
accountswitcher-2.6.1+1.20.6.jar
accountswitcher-2.6.1+1.21.1.jar
accountswitcher-2.6.1+1.21.11.jar
```

## Installing

Drop the jar matching your Minecraft version into your Fabric `mods` folder. Requires Fabric Loader 0.19.5 or newer and Fabric API.

## Project structure

The mod is built with [Stonecutter](https://github.com/stonecutter-versioning/stonecutter), a Gradle plugin that lets one shared source tree target multiple Minecraft versions. Source code lives once under `src/main/java` and `src/main/resources`. Places where the code differs between Minecraft versions are marked with `//? if <condition>` comments, which Stonecutter expands into the correct version specific code when building each target.

Key files:

- `stonecutter.properties.toml`: per version Yarn mappings, Fabric API version, and mod compatibility string
- `stonecutter.gradle.kts`: which version is active for local IDE editing
- `build.gradle.kts`: shared build script used by every version subproject

Adding a new Minecraft version means adding an entry to `stonecutter.properties.toml` and `settings.gradle.kts`, then fixing whatever compile errors show up for that version.
