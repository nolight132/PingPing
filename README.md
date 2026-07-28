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
./gradlew build
```

The jar lands in `build/libs/`.
