from PIL import Image

b_img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\SUNNYSIDE_WORLD_BUILDINGS_V0.01.png")
print("Buildings sheet size:", b_img.size)

# Let's crop individual houses / buildings or examine their positions!
# Let's find non-transparent bounding boxes or regions
import numpy as np
arr = np.array(b_img)
alpha = arr[:, :, 3]
print("Has alpha:", alpha.shape)
