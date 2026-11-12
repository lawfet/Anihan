import zipfile
import os

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"

for zname in ["media_1794410959884_2ae9eef3.zip", "media_1794410959884_c0220e0e.zip"]:
    zp = os.path.join(uploaded_dir, zname)
    with zipfile.ZipFile(zp, 'r') as z:
        print(f"\n--- Checking {zname} ---")
        for item in z.namelist():
            if "tileset" in item.lower() and not item.startswith("__MACOSX"):
                print(" ", item)
