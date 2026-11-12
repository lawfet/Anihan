from PIL import Image
import numpy as np

for name in ["screenshot_menu.png", "screenshot_play.png", "screenshot_pause.png"]:
    img = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\\" + name)
    arr = np.array(img)
    print(f"\n--- {name} ({img.size}) ---")
    # Check unique colors count
    print("Unique colors:", len(np.unique(arr.reshape(-1, arr.shape[2]), axis=0)))
