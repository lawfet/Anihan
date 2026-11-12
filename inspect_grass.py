from PIL import Image

tileset = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\maps\tileset.png")
# Let's save a zoomed-in 10x10 tile region of the tileset
crop = tileset.crop((0, 0, 160, 160))
crop_scaled = crop.resize((480, 480), Image.Resampling.NEAREST)
crop_scaled.save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\tileset_topleft.png")
print("Saved tileset_topleft.png")

# Also check other regions: dirt path, water, fences, trees, bridges
for tx, ty in [(0, 0), (1, 1), (2, 2), (3, 7), (10, 5), (11, 28), (20, 3)]:
    t = tileset.crop((tx*16, ty*16, tx*16+16, ty*16+16))
    print(f"Tile ({tx}, {ty}): size={t.size}, colors={len(t.getcolors() or [])}")
