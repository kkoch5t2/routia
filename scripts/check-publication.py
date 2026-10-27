#!/usr/bin/env python3
"""Inspect the staged tree or a committed tree without displaying matched values."""
import argparse
import re
import subprocess
import sys
from pathlib import PurePosixPath

def git(*args):
    return subprocess.check_output(["git", *args])

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--ref", help="Committed tree to inspect; defaults to the index")
args = parser.parse_args()
if args.ref:
    rows = git("ls-tree", "-rz", "--full-tree", args.ref).split(b"\0")
    files = []
    for row in rows:
        if row:
            meta, path = row.split(b"\t", 1)
            mode, kind, oid = meta.split()
            files.append((path.decode(), mode, oid.decode()))
else:
    rows = git("ls-files", "--stage", "-z").split(b"\0")
    files = []
    for row in rows:
        if row:
            meta, path = row.split(b"\t", 1)
            mode, oid, stage = meta.split()
            if stage != b"0":
                raise SystemExit("Resolve merge conflicts before publication.")
            files.append((path.decode(), mode, oid.decode()))
if not files:
    raise SystemExit("No staged or committed files to inspect.")

patterns = {
    "private key": re.compile(rb"-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----"),
    "provider credential": re.compile(
        rb"\b(?:sk-(?:proj-)?[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{20,}|"
        rb"github_pat_[A-Za-z0-9_]{20,}|AIza[A-Za-z0-9_-]{30,}|AKIA[A-Z0-9]{16})\b"),
    "local user path": re.compile(rb"(?:/h[o]me/|/Us[e]rs/|[A-Z]:\\Users\\)[^\s\"<>]+"),
    "private network address": re.compile(
        rb"\b(?:192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|"
        rb"172\.(?:1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})\b"),
    "credential literal": re.compile(
        rb"""(?i)(?:api[_-]?key|password|passwd|client[_-]?secret|access[_-]?token)\s*[:=]\s*["'][^"']{8,}["']"""),
}
email = re.compile(rb"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
forbidden_name = re.compile(
    r"(?i)(?:^\.env(?:\.|$)|\.(?:jks|keystore|p12|pfx|pem|key|db|sqlite|sqlite3|"
    r"apk|aab|idsig|zip|tgz|log|hprof)$|^local\.properties$|^(?:key|keystore)\.properties$|"
    r"^credentials.*\.json$|^service-account.*\.json$)")
problems = []
if args.ref:
    identities = git("log", "--format=%ae%n%ce", args.ref).decode().splitlines()
else:
    identities = []
    for role in ("GIT_AUTHOR_IDENT", "GIT_COMMITTER_IDENT"):
        match = re.search(r"<([^>]+)>", git("var", role).decode())
        identities.append(match.group(1) if match else "")
if any(not address.endswith("@users.noreply.github.com") for address in identities if address):
    problems.append(("(commit metadata)", 0, "use the account's GitHub noreply email"))
for path, mode, oid in files:
    parts = PurePosixPath(path).parts
    if mode in (b"120000", b"160000"):
        problems.append((path, 0, "symlink or submodule requires separate review"))
        continue
    if any(p in {"build", ".gradle", ".idea", "captures", "__pycache__"} for p in parts) or forbidden_name.search(parts[-1]):
        problems.append((path, 0, "private/local/generated file"))
    content = git("cat-file", "blob", oid)
    if b"\0" in content[:8192] or path.endswith((".webp", ".png", ".jpg", ".jpeg", ".jar")):
        continue
    for label, pattern in patterns.items():
        for match in pattern.finditer(content):
            problems.append((path, content[:match.start()].count(b"\n") + 1, label))
    for match in email.finditer(content):
        address = match.group().lower()
        if address.endswith((b"@users.noreply.github.com", b"@example.com")):
            continue
        problems.append((path, content[:match.start()].count(b"\n") + 1, "email address"))
if problems:
    for path, line, label in problems:
        print(f"{path}:{line}: {label}")
    raise SystemExit(f"Publication check failed: {len(problems)} finding(s); values suppressed.")
print(f"PASS: {len(files)} staged/committed files checked; no publication findings.")
