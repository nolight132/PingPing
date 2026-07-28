# PingPing

A simple multiplayer ping system for Minecraft. Middle-click to mark something; everyone on the server who has
the mod installed sees the marker for a few seconds.

- **Loader:** Fabric
- **Minecraft:** 26.1 – 26.2
- **Java:** 25
- **Sides:** required on both server and client
- **Depends on:** Fabric API
- **Optional:** YetAnotherConfigLib and Mod Menu, client-side, for the settings screen

## Controls

| Input | Action |
|---|---|
| Middle click | Mark the entity you are looking at. With nothing alive in sight, vanilla pick block gets the click, and failing that a bare marker lands on the exact spot aimed at. |
| Sneak + middle click | Mark the block you are looking at, previewed with its item icon. |

## Building

```sh
./gradlew build   # 26.2
./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.2+26.1.2 -Pyacl_version=3.9.6+26.1-fabric
```

Jars land in `build/libs/` named per game version. Gradle downloads a JDK 25 toolchain on its own, so no
system JDK is required.

Separate jars per game version, since YACL and Fabric API are built per version. The mod's own source is shared.

Settings live in `config/pingping.json` and are edited through the in-game screen (Mod Menu → PingPing). YACL is
client-only, so it is a soft dependency: without it the button simply does not appear and the file still works.
That also keeps the mod loadable on dedicated servers, which must not have YACL installed. The
limits — distance, lifetime, ping budget — are enforced by whichever side runs the logical server, so on a
dedicated server its own config file is the one that counts.
