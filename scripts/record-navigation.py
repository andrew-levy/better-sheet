#!/usr/bin/env python3
"""Record identical real app scenarios, including system bars; requires a booted adb device."""
from pathlib import Path
import argparse
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'dev.better.sheet.sample'
APP = ROOT / 'samples/sample/build/outputs/apk'
OUT = ROOT / 'docs/videos/navigation'

def run(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--skip-build', action='store_true')
    args = parser.parse_args()
    if not args.skip_build:
        run(str(ROOT / 'gradlew'), ':sample:assembleDebug', ':sample:assembleDebugAndroidTest', cwd=ROOT)
        run(str(ROOT / 'gradlew'), '--stop', cwd=ROOT)
    OUT.mkdir(parents=True, exist_ok=True)
    run('adb', 'wait-for-device')
    run('adb', 'install', '-r', str(APP / 'debug/sample-debug.apk'))
    run('adb', 'install', '-r', str(APP / 'androidTest/debug/sample-debug-androidTest.apk'))
    clips = []
    with tempfile.TemporaryDirectory() as temp:
        for approach in ['legacy', 'modal', 'adapter']:
            run('adb', 'shell', 'am', 'force-stop', PACKAGE)
            result = run('adb', 'shell', 'am', 'instrument', '-w', '-e', 'class',
                f'{PACKAGE}.NavigationRecordingTest#compareOpening', '-e', 'approach', approach,
                f'{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner',
                capture_output=True, text=True, timeout=900)
            print(result.stdout, flush=True)
            if 'OK (1 test)' not in result.stdout:
                raise RuntimeError(f'{approach} recording failed')
            run('adb', 'pull', f'/sdcard/Android/data/{PACKAGE}/files/navigation-frames/{approach}', temp)
            clip = OUT / f'{approach}.mp4'
            clips.append(clip)
            run('ffmpeg', '-v', 'error', '-y', '-framerate', '31.25', '-i',
                f'{temp}/{approach}/%05d.jpg', '-vf', 'fps=30', '-c:v', 'libx264',
                '-pix_fmt', 'yuv420p', '-crf', '18', '-movflags', '+faststart', str(clip))
    # Preserve full device frames in each column, with labels outside them.
    labels = ['1 - Existing DialogFragment', '2 - Custom sheet navigator', '3 - Dialog window adapter']
    filters = []
    for i, label in enumerate(labels):
        filters.append(f"[{i}:v]scale=540:-2,pad=iw:ih+72:0:72:color=0x172033,drawtext=text='{label}':fontcolor=white:fontsize=22:x=(w-tw)/2:y=24[v{i}]")
    filters.append('[v0][v1][v2]hstack=inputs=3[out]')
    inputs = [arg for clip in clips for arg in ['-i', str(clip)]]
    run('ffmpeg', '-v', 'error', '-y', *inputs, '-filter_complex', ';'.join(filters),
        '-map', '[out]', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '18',
        '-movflags', '+faststart', str(OUT / 'comparison.mp4'))

if __name__ == '__main__':
    main()
