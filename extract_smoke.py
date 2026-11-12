import zipfile
import io
import os
from PIL import Image

uploaded_dir = r"C:\Users\John Lawrence\.gemini\antigravity\brain\341b5d9c-8bac-46fe-a4a3-30cdabf5e71b\.user_uploaded"
outer_zip = os.path.join(uploaded_dir, "media_1794410959884_c0220e0e.zip")

with zipfile.ZipFile(outer_zip, 'r') as z:
    s_bytes = z.read("SUNNYSIDE_WORLD_CHIMNEYSMOKE_v1.0.zip")
    with zipfile.ZipFile(io.BytesIO(s_bytes)) as s_zip:
        for name in s_zip.namelist():
            if "chimneysmoke_01_strip30.png" in name:
                data = s_zip.read(name)
                out_path = r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\chimneysmoke_01_strip30.png"
                with open(out_path, "wb") as f:
                    f.write(data)
                img = Image.open(io.BytesIO(data))
                print("Saved chimney smoke, size:", img.size)
