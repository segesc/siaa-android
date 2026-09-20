#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, sys
root = Path(__file__).resolve().parents[1]
required = [
    'settings.gradle.kts','build.gradle.kts','gradle/libs.versions.toml',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/com/siaa/app/media/SiaaPlaybackService.kt',
    'core/runtime/src/main/kotlin/com/siaa/core/runtime/LessonRuntime.kt',
    'core/algorithm/src/main/kotlin/com/siaa/core/algorithm/AdaptiveUtilityPlanner.kt',
    'core/data/src/main/java/com/siaa/core/data/SiaaDatabase.kt',
    'reference/SIAA_documento_maestro_v1_5_android_v0_7.pdf'
]
missing=[x for x in required if not (root/x).exists()]
if missing:
    print('MISSING:', *missing, sep='\n- '); sys.exit(1)
manifest=json.loads((root/'project_manifest.json').read_text())
assert manifest.get('project') == 'SIAA'
pdf=root/'reference/SIAA_documento_maestro_v1_5_android_v0_7.pdf'
digest=hashlib.sha256(pdf.read_bytes()).hexdigest()
expected=(root/'reference/MASTER_PDF_SHA256.txt').read_text().strip()
assert digest == expected, 'Master PDF hash mismatch'
print('release verification OK')
print('files:', sum(1 for p in root.rglob('*') if p.is_file()))
