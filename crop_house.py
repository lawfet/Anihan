from PIL import Image

b_img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\SUNNYSIDE_WORLD_BUILDINGS_V0.01.png")
house = b_img.crop((0, 0, 305, 215))
house.save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\farmhouse.png")
print("Saved farmhouse.png, size:", house.size)
