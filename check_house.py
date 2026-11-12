from PIL import Image

house = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\farmhouse.png")
print("House size:", house.size)
# Let's save a preview with green background to verify it looks like a complete farmhouse
bg = Image.new("RGBA", (400, 300), (95, 193, 76, 255))
bg.paste(house, (40, 40), house)
bg.save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\house_preview.png")
print("Saved house_preview.png")
