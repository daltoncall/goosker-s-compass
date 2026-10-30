"""Fail CI if BossBarHudMixin's @Mixin annotation was packaged incorrectly.

Sponge Mixin reads its class-level @Mixin from RuntimeInvisibleAnnotations,
not RuntimeVisibleAnnotations. This catches a mistake that can compile cleanly
but crash Minecraft at preLaunch.
"""
import glob
import subprocess
import sys
from pathlib import Path

jars = [Path(p) for p in glob.glob('build/libs/*.jar') if not p.endswith('-sources.jar')]
if not jars:
    raise SystemExit('ERROR: No compiled mod JAR found in build/libs')

for jar in jars:
    output = subprocess.check_output([
        'javap', '-v', '-p', '-classpath', str(jar),
        'com.goosker.compass.client.mixin.BossBarHudMixin',
    ], text=True)
    class_annotations = output.rsplit('\nSourceFile:', 1)[-1]
    if ('RuntimeInvisibleAnnotations:' not in class_annotations
            or 'org.spongepowered.asm.mixin.Mixin(' not in class_annotations):
        raise SystemExit(f'ERROR: Invalid class-level @Mixin metadata in {jar}')
    print(f'PASS: {jar.name} includes a runtime-invisible @Mixin annotation')
