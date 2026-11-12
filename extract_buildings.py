import zipfile
import io
import os
from PIL import Image

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"
outer_zip = os.path.join(uploaded_dir, "media_1794410959884_c0220e0e.zip")

with zipfile.ZipFile(outer_zip, 'r') as z:
    b_bytes = z.read("SUNNYSIDE_WORLD_BUILDINGS_V0.01.zip")
    with zipfile.ZipFile(io.BytesIO(b_bytes)) as b_zip:
        for name in b_zip.namelist():
            if name.startswith("__MACOSX"): continue
            print("Buildings file:", name)
            if name.endswith(".png"):
                img = Image.open(io.BytesIO(b_zip.read(name)))
                print(" -> size:", img.size)
                out_path = os.path.join(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites", os.path.basename(name))
                img.save(out_path)
                print(" Saved to:", out_path)
