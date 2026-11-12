from PIL import Image

tileset = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\maps\tileset.png")
w, h = tileset.size
cw = w // 16
ch = h // 16

print(f"Tileset grid: {cw} x {ch} tiles")
rich_tiles = []
for ty in range(ch):
    for tx in range(cw):
        t = tileset.crop((tx*16, ty*16, tx*16+16, ty*16+16))
        cols = t.getcolors(256)
        if cols and len(cols) >= 12: # Rich detail tile
            # check not fully transparent
            non_trans = [c for c in cols if len(c[1]) < 4 or c[1][3] > 100]
            if len(non_trans) >= 10:
                rich_tiles.append((tx, ty, len(cols)))

print(f"Total rich detail tiles: {len(rich_tiles)}")
print("Sample rich tiles:", rich_tiles[:30])
