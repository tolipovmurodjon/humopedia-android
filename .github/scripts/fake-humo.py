#!/usr/bin/env python3
"""A fake Humopedia backend for the CI emulator (PAGEL's fake-pagel.py idea).

  fake-humo.py serve PORT LOG            answer the registerPushDevice callable and log every request as a JSON line
  fake-humo.py assert-requests LOG VER   fail unless the log holds exactly the requests the learner journey should send
  fake-humo.py assert-connections TCP UID  fail if the app's sockets (/proc/net/tcp dump) reach any host but the emulator's 10.0.2.2

The app's only backend call is the Firebase callable registerPushDevice (DailyPush.kt); progress stays on the phone.
The smoke build sends it to this server through BuildConfig.SMOKE_BACKEND; no other build ever does.
"""
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PATH = "/humo-ci/asia-south1/registerPushDevice"
BODY_KEYS = {"fid", "token", "sendId", "appVersion"}


def serve(port, log_path):
    log = open(log_path, "a", buffering=1)

    class Handler(BaseHTTPRequestHandler):
        def _record(self, body):
            log.write(json.dumps({"t": time.time(), "method": self.command, "path": self.path, "body": body}) + "\n")

        def do_GET(self):
            self._record(None)
            self.send_response(200 if self.path == "/health" else 404)
            self.end_headers()

        def do_POST(self):
            raw = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode("utf8", "replace")
            try:
                body = json.loads(raw)
            except ValueError:
                body = raw
            self._record(body)
            ok = self.path == PATH
            out = json.dumps({"result": {"ok": True}} if ok else {"error": {"status": "NOT_FOUND", "message": "unknown"}}).encode()
            self.send_response(200 if ok else 404)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(out)))
            self.end_headers()
            self.wfile.write(out)

        def log_message(self, *args):
            pass

    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()


def assert_requests(log_path, version, lo=None, hi=3):
    """One registerPushDevice per cold start. The walkthrough cold-starts the app 3 times (first launch, the forced
    crash, the motion phase); the crash run can die before its call leaves, so 2 to 3 are right."""
    import os
    if lo is None:
        lo = 1 if os.environ.get("QUICK") == "1" else 2
        hi = 1 if os.environ.get("QUICK") == "1" else hi
    rows = [json.loads(line) for line in open(log_path) if line.strip()]
    posts = [r for r in rows if r["path"] != "/health"]
    errors = []
    for r in posts:
        if r["method"] != "POST" or r["path"] != PATH:
            errors.append(f"unexpected request {r['method']} {r['path']}")
            continue
        data = (r["body"] or {}).get("data") if isinstance(r["body"], dict) else None
        if not isinstance(data, dict) or set(data) != BODY_KEYS:
            errors.append(f"body keys {sorted(data) if isinstance(data, dict) else r['body']} are not {sorted(BODY_KEYS)}")
            continue
        want = {"fid": "ci-fid", "token": "ci-token", "sendId": None, "appVersion": version}
        if data != want:
            errors.append(f"body {data} is not {want}")
    if not lo <= len(posts) <= hi:
        errors.append(f"{len(posts)} request(s), expected {lo} to {hi}")
    print(f"fake-humo: {len(posts)} request(s) recorded")
    for r in posts:
        print("  " + json.dumps({"method": r["method"], "path": r["path"], "body": r["body"]}))
    return errors


def assert_connections(tcp_path, uid):
    """Best effort: sockets alive at the moment of the dump, owned by the app's uid. Anything but 10.0.2.2 is a leak."""
    bad = []
    seen = 0
    for line in open(tcp_path):
        cols = line.split()
        if len(cols) < 8 or cols[0] == "sl" or cols[7] != str(uid):
            continue
        ip_hex, port_hex = cols[2].split(":")
        if int(ip_hex, 16) == 0 or cols[3] in ("0A",):  # unset address / listening
            continue
        seen += 1
        # IPv4 is stored little-endian: 10.0.2.2 reads 0202000A. IPv6 rows end with the IPv4-mapped part.
        if not ip_hex.endswith("0202000A"):
            raw = bytes.fromhex(ip_hex)
            bad.append(f"socket to {'.'.join(str(b) for b in raw[-4:][::-1])}:{int(port_hex, 16)} state {cols[3]}")
    print(f"fake-humo: {seen} live socket(s) of uid {uid} looked at")
    return bad


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "serve":
        serve(int(sys.argv[2]), sys.argv[3])
    elif cmd == "assert-requests":
        errs = assert_requests(sys.argv[2], sys.argv[3])
    elif cmd == "assert-connections":
        errs = assert_connections(sys.argv[2], sys.argv[3])
    else:
        sys.exit(__doc__)
    if cmd != "serve":
        for e in errs:
            print("FAIL: " + e)
        sys.exit(1 if errs else 0)
