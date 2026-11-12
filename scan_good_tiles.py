from PIL import Image

tileset = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\maps\tileset.png")

# Let's inspect rows 0 to 15, cols 0 to 20
# and save a clear annotated catalog
for y in range(0, 15):
    for x in range(0, 20):
        tile = tileset.crop((x*16, y*16, (x+1)*16, (y+1)*16))
        # check if it has variation
        colors = tile.getcolors(256)
        if colors and len(colors) > 3:
            # check non-transparent
            alpha_ok = all(len(c[1]) < 4 or c[1][3] > 200 for c in colors)
            if alpha_ok:
                print(f"Good tile at ({x}, {y}): {len(colors)} colors, dom={max(colors, key=lambda c: c[0])[1][:3]}")
