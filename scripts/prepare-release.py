#!/usr/bin/env python3
"""Prepare public update metadata only for an APK signed by the original key."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--notes-file', type=Path)
args = parser.parse_args()
version = dict(line.strip().split('=', 1) for line in (ROOT / 'version.properties').read_text().splitlines() if '=' in line)
name, code = version['versionName'], int(version['versionCode'])
if not re.fullmatch(r'\d+\.\d+\.\d+', name) or code < 1:
    raise SystemExit('Invalid version.properties')
apk = ROOT / 'dist' / f'neon-shift-{name}.apk'
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '35.0.0'
env = dict(os.environ)
env['PATH'] = str(Path(os.environ['JAVA_HOME']) / 'bin') + os.pathsep + env.get('PATH', '')
verified = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--print-certs', str(apk)], env=env, text=True)
fingerprint = re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)', verified)
expected = (ROOT / 'release-signing-cert.sha256').read_text().strip()
if not fingerprint or fingerprint.group(1) != expected:
    raise SystemExit('Refused: APK is not signed by the original Neon Shift key. Keep the private key local.')
badging = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
if not re.search(r"name='com\.neonshift\.app' versionCode='" + str(code) + r"' versionName='" + re.escape(name) + "'", badging):
    raise SystemExit('APK package/version does not match version.properties')
notes = args.notes_file.read_text() if args.notes_file else 'Melhorias e correções no Neon Shift.'
manifest = {
    'schemaVersion': 1, 'applicationId': 'com.neonshift.app',
    'versionCode': code, 'versionName': name, 'minSdk': 26,
    'apkUrl': f'https://github.com/levymtmr/menatwork/releases/download/v{name}/{apk.name}',
    'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'size': apk.stat().st_size,
    'notes': notes[:4000].strip(),
}
(ROOT / 'dist' / 'update.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
print(f'Prepared: {apk.name}, {apk.name}.sha256, update.json for v{name}')
