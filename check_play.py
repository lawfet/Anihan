from PIL import Image

img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\screenshot_play.png")
# Let's inspect center 200x200
crop = img.crop((400, 200, 600, 400))
crop.save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\play_center.png")

# Let's check what AssetManager loaded for tileset!
print("Checking AssetManager tileset...")
