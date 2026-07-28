# PingPing

Fortnite-style pings for Minecraft. Middle-click an entity to mark it; everyone on the server who has the mod
installed sees the marker for a few seconds.

- **Loader:** Fabric
- **Minecraft:** 26.1 – 26.2
- **Java:** 25
- **Sides:** required on both server and client

## Controls

| Input | Action |
|---|---|
| Middle click | Ping the entity you are looking at (up to 64 blocks). If no entity is targeted, vanilla pick block runs as usual. |
| Shift + middle click | Always ping, never pick block. |

## Building

```sh
./gradlew build                                                              # 26.2
./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.2+26.1.2   # 26.1
```

Jars land in `build/libs/` named per game version. Gradle downloads a JDK 25 toolchain on its own, so no
system JDK is required.

26.1 and 26.2 need separate jars: `SubmitNodeCollector#submitNameTag` lost its camera-distance argument in
26.2. That single call is the only divergence, and it lives in `src/client/java-26.1` and
`src/client/java-26.2`; everything else is shared.
