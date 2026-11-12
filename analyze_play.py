from PIL import Image

img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\screenshot_play.png")
colors = img.getcolors(maxcolors=10000)
# Sort by count
colors.sort(key=lambda x: x[0], reverse=True)
print("Top 15 colors in screenshot_play.png:")
for count, col in colors[:15]:
    print(f"  {count} pixels: RGBA={col}")
