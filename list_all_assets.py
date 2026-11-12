import zipfile
import os

pack_zip = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded\media_1794410959884_2ae9eef3.zip"
with zipfile.ZipFile(pack_zip, 'r') as z:
    for n in z.namelist():
        if not n.startswith("__MACOSX") and "Sunnyside_World_Assets" in n and n.endswith(".png"):
            print(n.replace("Sunnyside_World_ASSET_PACK_V2.1/Sunnyside_World_Assets/", ""))
