import os
import zipfile
import io
from PIL import Image

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"

# 1. Check output_tileset.png
pack_zip = os.path.join(uploaded_dir, "media_1794410959884_2ae9eef3.zip")
with zipfile.ZipFile(pack_zip, 'r') as z:
    for n in z.namelist():
        if "output_tileset.png" in n and not n.startswith("__MACOSX"):
            img = Image.open(io.BytesIO(z.read(n)))
            print("Found in pack:", n, "size:", img.size)

# 2. Check Sunnyside World_Tileset_Beta (1).zip
outer_zip = os.path.join(uploaded_dir, "media_1794410959884_c0220e0e.zip")
with zipfile.ZipFile(outer_zip, 'r') as z:
    inner_data = z.read("Sunnyside World_Tileset_Beta (1).zip")
    with zipfile.ZipFile(io.BytesIO(inner_data)) as inner:
        for n in inner.namelist():
            if n.endswith(".png") and not n.startswith("__MACOSX"):
                if "spr_tileset" in n or "tileset" in n:
                    img = Image.open(io.BytesIO(inner.read(n)))
                    print("Found in Beta zip:", n, "size:", img.size)
