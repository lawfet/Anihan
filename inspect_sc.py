from PIL import Image

sc = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\example_scene.png")
sc_small = sc.resize((1108, 617), Image.Resampling.BILINEAR)
sc_small.save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\scene_preview.png")
print("Saved scene_preview.png successfully")
