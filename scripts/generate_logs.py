#!/usr/bin/env python3
"""Generate a fake log file for demos.  Usage: python3 generate_logs.py 1000000 sample.log"""
import random
import sys
from datetime import datetime, timedelta

lines = int(sys.argv[1]) if len(sys.argv) > 1 else 100_000
out = sys.argv[2] if len(sys.argv) > 2 else "sample.log"

levels = ["INFO"] * 70 + ["DEBUG"] * 15 + ["WARN"] * 10 + ["ERROR"] * 5
services = ["auth", "payments", "orders", "gateway", "inventory"]
messages = {
    "INFO": ["request completed in {n} ms", "user {n} logged in", "cache refreshed"],
    "DEBUG": ["loading config for tenant {n}", "sql query took {n} ms"],
    "WARN": ["slow response from upstream ({n} ms)", "retrying request attempt {n}"],
    "ERROR": ["connection timeout after {n} ms", "NullPointerException in handler {n}",
              "database deadlock detected on table orders", "payment declined for user {n}"],
}

t = datetime(2024, 5, 1, 0, 0, 0)
with open(out, "w") as f:
    for _ in range(lines):
        t += timedelta(milliseconds=random.randint(1, 400))
        lvl = random.choice(levels)
        msg = random.choice(messages[lvl]).format(n=random.randint(1, 5000))
        f.write(f"{t:%Y-%m-%d %H:%M:%S} {lvl} [{random.choice(services)}] {msg}\n")
print(f"wrote {lines} lines to {out}")
