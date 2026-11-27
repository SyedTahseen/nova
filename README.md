# Nova VPN (Android, Jetpack Compose + Xray-core)

Three screens from the design: **Home** (map + region chips + slide-to-connect), **Connected** (plasma orb, timer, live download/upload), **Regions** (search, add, menu, your servers). The app ships with no servers: add yours with the **+** button; the flag and map position come from the proxy name.

## Before it will build

1. **Add the Xray core library.** Download `libv2ray.aar` (AndroidLibXrayLite, github.com/2dust/AndroidLibXrayLite → Releases) and put it at:
   `app/libs/libv2ray.aar`
2. **Add your servers** in `app/src/main/assets/regions.json`. Each region has a `link` share link. The shipped links are placeholders (the app says so when you try to connect) – add real ones here or with the **+** button on the Regions screen.
3. Open the folder in **Android Studio** (Ladybug or newer), let it sync (it will create the Gradle wrapper), then Run on a device. VPN does not work on most emulators.

## How it works

| Piece | File |
|---|---|
| Tunnel + foreground service | `vpn/NovaVpnService.kt` |
| UI ⇄ service state (status, timer, speed) | `vpn/VpnController.kt` |
| Only file touching the native lib | `core/XrayCore.kt` |
| Share link → Xray JSON | `core/ServerLink.kt`, `core/XrayConfigBuilder.kt` |
| Screens | `ui/screens/*` |
| Orb, slider, map | `ui/components/OrbView.kt`, `SlideToggle.kt`, `ui/map/*` |

- The app excludes itself from the VPN (`addDisallowedApplication`) so Xray's own connection to your server doesn't loop.
- Download/upload speed is sampled each second from this app's own socket counters (that is Xray's traffic).
- Signal icon colour on the Regions list is a live TCP ping to each server (green < 120 ms, orange < 250 ms, red slower, grey unreachable).
- Picking a different region while connected switches the tunnel without disconnecting first.

## Known gaps / things to check

- **Not compiled or run yet.** It was written without an Android SDK or network, so expect to fix a few compile errors on first sync.
- **Xray TUN API.** `XrayCore.kt` assumes a recent libv2ray that exposes `Libv2ray.newCoreController(...).startLoop(config, tunFd)` and an Xray `tun` inbound. If your AAR has an older API (`newV2RayPoint`/`runLoop`, SOCKS inbound + tun2socks), adapt `XrayCore.kt` and the inbound in `XrayConfigBuilder.kt`.
- **Map borders** come from Natural Earth (public domain) via `app/src/main/assets/world.bin`. Generate it once with `python3 tools/generate_world_map.py` and commit it (the GitHub workflow also generates it automatically if it is missing). Without that file the app falls back to a very coarse outline with no borders.
- **Font** is the system sans-serif; drop your design font into `res/font/` and set it in `ui/theme/Theme.kt`.
- "Premium regions" is only a grouping; there is no billing or locking. The banner's arrow currently shows a toast.
- Banner text reads "Europe, USA, Canada" (the mockup says "Europa").

## Supported protocols (share links)

`vless://`, `vmess://` (base64-JSON and URI style), `trojan://`, `ss://` (SIP002 + legacy), `socks5://`, `wireguard://`.
Transports: tcp, ws, grpc, httpupgrade, xhttp, kcp; security: none / tls / reality.
Not supported: Shadowsocks plugins, Hysteria / TUIC (need a different core).

## When a connection fails

Before starting the tunnel the app resolves the host and opens a TCP connection to it, and shows the reason if that fails.
Once connected it sends one real request through the tunnel; if that fails an orange warning appears on the Connected screen together with the last lines of Xray's error log (`files/xray.log`).
