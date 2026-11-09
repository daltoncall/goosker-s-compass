#!/usr/bin/env python3
"""CI integrity check for the persistent-death cache additions in beta-1.1.16."""
import subprocess
import zipfile
from pathlib import Path

jars=[]
for f in Path('build/libs').glob('*.jar'):
    with zipfile.ZipFile(f) as z:
        if ('com/goosker/compass/client/hud/MarkerManager.class' in z.namelist()
            and 'com/goosker/compass/client/hud/DeathWaypointCache.class' in z.namelist()
            and 'com/goosker/compass/client/hud/CompassHudRenderer.class' in z.namelist()):
            jars.append(f)
assert jars, 'Compiled JAR with all death-cache classes missing'
for jar in jars:
    def inspect(cls):
        return subprocess.check_output(['javap','-c','-p','-classpath',str(jar),cls], text=True)
    manager=inspect('com.goosker.compass.client.hud.MarkerManager')
    hud=inspect('com.goosker.compass.client.hud.CompassHudRenderer')
    assert 'DeathWaypointCache.merge:' in manager, f'Cache merge call missing from {jar}'
    assert 'DeathWaypointCache.shouldRenderDeathMarker:' in hud, f'Safe grave check missing from {jar}'
    print('PASS:',jar,'contains death waypoint caching and verified grave check')
