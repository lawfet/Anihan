from PIL import Image

d1 = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\itemdisc_01.png")
d2 = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\itemdisc_02.png")
print("itemdisc_01 size:", d1.size)
print("itemdisc_02 size:", d2.size)

# Tool icons
for tool in ["shovel.png", "water.png", "axe.png", "pickaxe.png", "basket.png", "plant.png"]:
    t_img = Image.open(rf"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\{tool}")
    print(f"Tool {tool} size:", t_img.size)
