from PIL import Image

sb = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\stat_bars.png")
print("stat_bars size:", sb.size)

# Inspect pixels
import numpy as np
arr = np.array(sb)
# Heart bar is top, lightning bar is bottom
# Let's find transparent vs non-transparent
print("Row 0 to 30 (Heart bar):")
# Where are pixels in top half
top_half = arr[:30, :, :]
# Save heart bar and bolt bar
sb.crop((0, 0, 192, 30)).save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\bar_heart.png")
sb.crop((0, 24, 192, 58)).save(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\bar_bolt.png")
print("Saved bar_heart.png and bar_bolt.png")
