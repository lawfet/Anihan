from PIL import Image

b_img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\SUNNYSIDE_WORLD_BUILDINGS_V0.01.png")
print("Size:", b_img.size)

# Let's save a preview with a dark checkerboard background
import numpy as np
arr = np.array(b_img)
print("Alpha channel max:", arr[:,:,3].max())
# Find bounding boxes of content
# Let's see how many houses/buildings are on the sheet
