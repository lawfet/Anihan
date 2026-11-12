import zipfile
import io
import os
from PIL import Image

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"
pack_zip = os.path.join(uploaded_dir, "media_1794410959884_2ae9eef3.zip")

with zipfile.ZipFile(pack_zip, 'r') as z:
    for n in z.namelist():
        if "tileset_sunnysideworld/output_tileset.png" in n and not n.startswith("__MACOSX"):
            data = z.read(n)
            img = Image.open(io.BytesIO(data))
            out_path = r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\maps\output_tileset.png"
            with open(out_path, "wb") as f:
                f.write(data)
            print("Extracted output_tileset.png:", img.size)

# Let's inspect what tiles are in output_tileset.png
tiles = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\maps\output_tileset.png")
print("Tiles size:", tiles.size)
