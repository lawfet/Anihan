import zipfile
import os

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"

# Check SUNNYSIDE_WORLD_BUILDINGS_V0.01.zip inside media_1794410959884_c0220e0e.zip
outer_zip = os.path.join(uploaded_dir, "media_1794410959884_c0220e0e.zip")
with zipfile.ZipFile(outer_zip, 'r') as z:
    for name in z.namelist():
        print("Outer zip entry:", name)

# Also check pack zip
pack_zip = os.path.join(uploaded_dir, "media_1794410959884_2ae9eef3.zip")
with zipfile.ZipFile(pack_zip, 'r') as z:
    for name in z.namelist():
        if ("building" in name.lower() or "house" in name.lower() or "farm" in name.lower() or "ui" in name.lower()) and not name.startswith("__MACOSX"):
            if name.endswith(".png"):
                print("Pack PNG:", name)
