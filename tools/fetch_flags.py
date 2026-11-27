#!/usr/bin/env python3
"""
Downloads round country flags (HatScripts/circle-flags, MIT licensed) for every ISO 3166 alpha-2 code
and writes them as 128x128 PNGs to app/src/main/assets/flags/<cc>.png.

The app picks them up automatically (see ui/components/Flag.kt) and falls back to the flag emoji for any
code that has no file, so you can also run this for just the countries you need:

    pip install cairosvg
    python tools/fetch_flags.py            # all countries
    python tools/fetch_flags.py gb de ca   # only these
"""
import sys
import urllib.request
from pathlib import Path

import cairosvg

CODES = "ac ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bl bm bn bo bq br bs bt bv bw by bz ca cc cd cf cg ch ci ck cl cm cn co cp cq cr cu cv cw cx cy cz de dg dj dk dm do dz ea ec ee eg eh er es et eu ez fi fj fk fm fo fr ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu ic id ie il im in io iq ir is it je jm jo jp ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mf mg mh mk ml mm mn mo mp mq mr ms mt mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk pl pm pn pr ps pt pw py qa qo re ro rs ru rw sa sb sc sd se sg sh si sj sk sl sm sn so sr ss st sv sx sy sz ta tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug um un us uy uz va vc ve vg vi vn vu wf ws xa xb xk ye yt za zm zw zz".split()
URL = "https://hatscripts.github.io/circle-flags/flags/{}.svg"
OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/flags"
SIZE = 128


def main() -> None:
    wanted = [c.lower() for c in sys.argv[1:]] or CODES
    OUT.mkdir(parents=True, exist_ok=True)
    ok, missing = 0, []
    for code in wanted:
        try:
            with urllib.request.urlopen(URL.format(code), timeout=20) as r:
                svg = r.read()
            cairosvg.svg2png(bytestring=svg, write_to=str(OUT / f"{code}.png"), output_width=SIZE, output_height=SIZE)
            ok += 1
        except Exception:
            missing.append(code)
    print(f"{ok} flags written to {OUT}")
    if missing:
        print("no flag for:", " ".join(missing))
    if ok == 0:
        sys.exit(1)  # nothing downloaded at all (offline / blocked): fail loudly instead of shipping emoji only


if __name__ == "__main__":
    main()
