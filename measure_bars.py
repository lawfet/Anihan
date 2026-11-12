from PIL import Image
import numpy as np

arr = np.array(Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\stat_bars.png"))
# Check row 12 for heart bar (y=12)
row12 = arr[12, :, :]
print("Row 12 colors across x:")
for x in range(25, 190, 5):
    print(f" x={x}: RGBA={tuple(row12[x])}")

# Check row 40 for bolt bar (y=40)
row40 = arr[40, :, :]
print("\nRow 40 colors across x:")
for x in range(25, 160, 5):
    print(f" x={x}: RGBA={tuple(row40[x])}")
