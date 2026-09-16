"""Build APKs using an external, private signing configuration."""
import json, os, subprocess
from pathlib import Path
root = Path(__file__).resolve().parents[1]
config = Path(os.environ.get("GATARME_SIGNING_CONFIG", str(Path.home() / ".gatarme-signing/signing.json")))
env = os.environ.copy()
if config.exists(): env.update(json.loads(config.read_text()))
subprocess.run([str(root / ("gradlew.bat" if os.name == "nt" else "gradlew")), "assembleDebug", "assembleRelease", "lintDebug", "--console", "plain"], cwd=root, env=env, check=True)
