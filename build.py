from pathlib import Path
import os
import shutil
import subprocess
import sys

project_dir = Path(__file__).resolve().parent
bin_dir = project_dir / "bin"
bin_dir.mkdir(exist_ok=True)
java_home = os.environ.get("JAVA_HOME")
compiler = str(Path(java_home) / "bin" / "javac") if java_home else shutil.which("javac")
if not compiler:
    sys.exit("Install a JDK 17 or newer and put javac on PATH or set JAVA_HOME.")
java_files = sorted(str(path) for path in (project_dir / "src").rglob("*.java"))
result = subprocess.run([compiler, "-Xlint:all", "-d", str(bin_dir), *java_files], cwd=project_dir)
sys.exit(result.returncode)
