from PIL import Image
import numpy as np

b_img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\sprites\SUNNYSIDE_WORLD_BUILDINGS_V0.01.png")
arr = np.array(b_img)

# Let's inspect where pixels exist
has_pixel = (arr[:, :, 3] > 10)
# Print bounds of columns and rows that have pixels
row_sums = has_pixel.sum(axis=1)
col_sums = has_pixel.sum(axis=0)

print("Rows with pixels:", np.where(row_sums > 0)[0][0], "to", np.where(row_sums > 0)[0][-1])
print("Cols with pixels:", np.where(col_sums > 0)[0][0], "to", np.where(col_sums > 0)[0][-1])

# Let's crop the main farmhouse
# In Sunnyside World buildings, usually there are 2-3 houses on the sheet
# Let's check non-empty slices
import scipy.ndimage as ndi
lbl, num = ndi.label(has_pixel)
print(f"Connected components: {num}")
for i in range(1, min(num+1, 10)):
    pos = np.where(lbl == i)
    ymin, ymax = pos[0].min(), pos[0].max()
    xmin, xmax = pos[1].min(), pos[1].max()
    w = xmax - xmin + 1
    h = ymax - ymin + 1
    if w > 40 and h > 40:
        print(f"Component {i}: box=({xmin}, {ymin}, {xmax}, {ymax}), w={w}, h={h}")
        crop = b_img.crop((xmin, ymin, xmax+1, ymax+1))
        crop.save(rf"C:\Users\John Lawrence\.gemini\antigravity\scratch\building_{i}.png")
