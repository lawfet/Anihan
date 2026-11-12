from pathlib import Path
import os
import shutil
import subprocess
import sys

project_dir = Path(__file__).resolve().parent
subprocess.run([sys.executable, str(project_dir / "build.py")], cwd=project_dir, check=True)
java_home = os.environ.get("JAVA_HOME")
compiler = shutil.which("javac")
if java_home:
    java_dir = Path(java_home) / "bin"
elif compiler:
    java_dir = Path(compiler).parent
else:
    sys.exit("Install a JDK 17 or newer before capturing the game.")
java = java_dir / ("java.exe" if os.name == "nt" else "java")
result = subprocess.run([str(java), "-cp", str(project_dir / "bin"), "engine.RenderTest", *sys.argv[1:]], cwd=project_dir)
sys.exit(result.returncode)
