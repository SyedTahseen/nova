# Nova VPN (Android, Jetpack Compose + Xray-core)

Three screens from the design: **Home** (map + region chips + slide-to-connect), **Connected** (plasma orb, timer, live download/upload), **Regions** (promo banner, free + premium lists).

## Before it will build

1. **Add the Xray core library.** Download `libv2ray.aar` (AndroidLibXrayLite, github.com/2dust/AndroidLibXrayLite → Releases) and put it at:
   `app/libs/libv2ray.aar`
2. **Add your servers** in `app/src/main/assets/regions.json`. Each region has a `link` – a `vless://` or `trojan://` share link (tcp / ws / grpc / httpupgrade, with tls or reality). The shipped links are placeholders and will not connect.
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
- **Map** is a hand-drawn coarse outline with a square grid, not the country-border map in your design. Export your map layer and swap the polygons in `ui/map/WorldGeo.kt` (or draw it as a vector drawable).
- **Font** is the system sans-serif; drop your design font into `res/font/` and set it in `ui/theme/Theme.kt`.
- "Premium regions" is only a grouping; there is no billing or locking. The banner's arrow currently shows a toast.
- Banner text reads "Europe, USA, Canada" (the mockup says "Europa").
